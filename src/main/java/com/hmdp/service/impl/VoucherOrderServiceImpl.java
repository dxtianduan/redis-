package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.ReidsWorker;
import com.hmdp.utils.UserHolder;
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
    @Override
    @Transactional
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
        //判断库存
        if (voucher.getStock()<1){
            return Result.fail("库存不足" );
        }
        //扣减库存
 boolean success= iSeckillVoucherService.update()
        .setSql("stock=stock-1")
        .eq("voucher_id",voucherId).update();

if (!success){
    //扣减失败
    return Result.fail("库存不足" );
}
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
//订单ID
        long order = reidsWorker.nextId("order");
        voucherOrder.setId(order);
        //用户ID
        //【修复4】判空：正常情况下 LoginInterCeptor 已经拦掉了未登录请求，
        // 但 mvcConfig 里 /voucher-order/** 不在放行名单，若将来有人改了白名单，
        // 这里会直接 NPE。显式判一下，报错信息对初学者更友好。
        com.hmdp.dto.UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("未登录，请先登录后再抢购");
        }
        Long userid = currentUser.getId();
        voucherOrder.setUserId(userid);
        //代金券ID
voucherOrder.setVoucherId(voucherId);

save(voucherOrder);
        //返回订单id
        return Result.ok(order);
    }
}
