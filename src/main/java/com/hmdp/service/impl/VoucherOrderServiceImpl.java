package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.ReidsWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private ReidsWorker reidsWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 秒杀下单 —— 第一层：只做「安全检查」
     *
     * 【重要】这个方法上故意不加 @Transactional，它只负责快速失败：
     * 不是秒杀券、活动没开始、活动已结束、库存没了 —— 这些情况直接返回，不碰数据库写入。
     * 真正的「扣库存 + 建订单」放在 createVoucherOrder 里，由代理对象调用，事务才生效。
     */
    @Override
    public Result killVoucher(Long voucherId) {
        //查询：voucherId 是"券id"，要在 tb_seckill_voucher 里查这张券的库存和起止时间
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //【修复1】判空：如果这张券根本没有秒杀记录(不是秒杀券)，getById 会返回 null，
        // 后面直接 .getBeginTime() 就是 NullPointerException，被全局异常处理器吞成"服务器异常"
        if (voucher == null) {
            return Result.fail("该优惠券不是秒杀券或不存在");
        }
        //【修复2】判断秒杀是否开始：用 beginTime，当前时间在开始时间之前 -> 还没开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //【修复3】判断秒杀是否结束：必须用 endTime！原来写的是 getBeginTime()，
        // 导致"开始时间早于现在"恒成立，秒杀一开始就被判成已结束，永远抢不到
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        //【修复6】这里只做"预检查"，能提前把没货的请求挡掉；
        // 真正决定能不能扣成功的是下面 createVoucherOrder 里带 WHERE stock > 0 的 UPDATE
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        //【修复7】未登录时 UserHolder.getUser() 是 null，先取出来判空再解引用
        com.hmdp.dto.UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("未登录，请先登录后再抢购");
        }
        Long id = currentUser.getId();

        //创建锁对象
        SimpleRedisLock Lock = new SimpleRedisLock("Order:" + id, stringRedisTemplate);
        //获取锁
        boolean tryLock = Lock.tryLock(1200);
        //判断是否获取成功
        if (!tryLock){
            //失败
            return Result.fail("不允许重复下单");

        }


//获取成功
        //【关键】必须用代理对象调用，不能写 this.createVoucherOrder(...)
            // 因为 this 是原始对象，绕过了 Spring 的 AOP 代理，@Transactional 会完全失效。
            // AopContext.currentProxy() 能拿到代理对象，前提是启动类上有
            // @EnableAspectJAutoProxy(exposeProxy = true)
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            Lock.unlock();
        }

    }

    /**
     * 秒杀下单 —— 第二层：真正写数据库
     *
     * 这个方法里的三件事（一人一单判断、扣库存、建订单）必须是一个整体：
     * 任何一步失败，整段都要回滚，不能出现"订单没建成、库存却少了"的情况。
     *
     * 【注意】这个方法只有在被"代理对象"调用时，@Transactional 才会生效。
     * 写成 this.createVoucherOrder(...) 是自调用，事务不生效 —— 这是本课程最容易踩的坑之一。
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //一人一单
        Long id = UserHolder.getUser().getId();
        //【修复4】判空：正常情况下 LoginInterCeptor 已经拦掉了未登录请求，
        // 但 mvcConfig 里 /voucher-order/** 不在放行名单，若将来有人改了白名单，
        // 这里会直接 NPE。显式判一下，报错信息对初学者更友好。
        if (id == null) {
            return Result.fail("未登录，请先登录后再抢购");
        }

        //查询订单
        int count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        //判断是否存在
        if (count > 0) {
            //用户购买过
            return Result.fail("用户购买过,一人只能购买一单");
        }

        //【修复5】扣减库存 —— 放在"一人一单判断之后、建订单之前"，和建订单在同一个事务里
        // 乐观锁：把"库存>0"这个条件塞进 SQL 的 WHERE 里，靠数据库行锁保证不超卖。
        // 原代码有两个问题：
        //   ① 列名拼错：写成了 "stoke"（正确的是 "stock"）
        //      → MySQL 直接报 Unknown column 'stoke' in 'where clause'，被全局异常处理器吞成"服务器异常"
        //   ② 比较条件写反：.gt("stock", voucher.getStock()) 的意思是"库存 > 我刚刚读到的那个值"，
        //      而库存不可能大于它自己，所以这个条件永远不成立 → 恒返回"库存不足"
        //      正确写法是 .gt("stock", 0)：只要库存还大于 0 就允许扣减
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();

        if (!success) {
            //扣减失败
            return Result.fail("库存不足");
        }

        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单ID
        long order = reidsWorker.nextId("order");
        voucherOrder.setId(order);
        //用户ID
        Long userid = UserHolder.getUser().getId();
        voucherOrder.setUserId(userid);
        //代金券ID
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);
        //返回订单id
        return Result.ok(order);
    }
}
