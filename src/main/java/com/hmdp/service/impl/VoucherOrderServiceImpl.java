package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.ReidsWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private ReidsWorker reidsWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 用来在运行时按类型取出本 Bean 的代理对象（见下面 selfProxy() 的说明） */
    @Resource
    private ApplicationContext applicationContext;

    /**
     * 这里要注入的是 Bean 本身，不是创建 Bean 的那个配置类。
     *
     * Redissionclient 是 @Configuration 配置类（相当于"造锁的工厂"），
     * 它里面 @Bean 方法 redissonClient() 返回的 RedissonClient 才是"产品"。
     * 工厂上并没有 getLock() 方法，所以注入配置类之后 IDE 找不到 getLock。
     *
     * 按"类型"注入最省心：Spring 会去容器里找类型是 RedissonClient 的 Bean。
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 秒杀资格预判断脚本。返回值：0 = 有购买资格，1 = 库存不足，2 = 重复下单。
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        // 【本次修复】这里必须指向 seckill.lua。
        // 原来写的是 unlock.lua —— 那是"释放锁"的脚本，被当成秒杀脚本执行，
        // 脚本里读 KEYS[1] 时拿不到值，Redis 直接抛错，
        // 异常一路冒到 WebExceptionAdvice，前端看到的就是"服务器异常"。
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 订单阻塞队列：Lua 判断通过的订单先塞进这里，由后台线程慢慢落库。
     *
     * 好处：接口只做一次 Redis 调用就能返回，不再等 MySQL 写入，
     * 响应时间从"几十毫秒"降到"几毫秒"，扛并发的能力完全不同。
     */
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    /**
     * 消费队列的线程池。
     *
     * ★ 核心线程数必须只有 1 个。
     *   如果开多个线程一起落库，"一人一单"的判断和库存扣减就会重新出现并发竞态，
     *   等于白折腾一趟 Redis 预判断。串行处理虽然慢，但天然不会有竞态。
     *
     * ★ 线程要有名字：出问题时用 jstack 能一眼认出是哪个线程卡住了。
     * ★ 守护线程：主程序退出时不会因为它在 while(true) 里而无法结束。
     * ★ 队列有上限：防止无限堆积把内存撑爆。
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            r -> {
                Thread t = new Thread(r, "seckill-order-handler");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 代理对象（懒加载）。
     *
     * 【为什么必须懒加载，不能在 @PostConstruct 里直接赋值】
     * 实测在 @PostConstruct 里调用 AopContext.currentProxy() 会抛异常，应用直接启动失败：
     *   IllegalStateException: Cannot find current proxy: Set 'exposeProxy' property on
     *   Advised to 'true' to make it available, and ensure that AopContext.currentProxy()
     *   is invoked in the same thread as the AOP invocation context.
     *
     * 原因有两个：
     *   1. @PostConstruct 执行的时机，比 Spring 给这个 Bean 生成代理对象还要早，
     *      那一刻代理压根不存在（就算加了 exposeProxy = true 也没用）；
     *   2. AopContext 底层是 ThreadLocal，只有在"AOP 调用上下文"里才有值。
     *
     * 所以改成"第一次真正用到时，再去容器里拿" —— 那时 Bean 早就创建完成，
     * 拿到的一定是代理对象，事务注解也就一定生效。
     */
    private volatile IVoucherOrderService proxy;

    private IVoucherOrderService selfProxy() {
        IVoucherOrderService p = proxy;
        if (p == null) {
            p = applicationContext.getBean(IVoucherOrderService.class);
            proxy = p;
        }
        return p;
    }

    /**
     * 启动时把"消费队列的后台线程"拉起来。
     * 注意这里只负责启动线程，取代理对象交给 selfProxy() 懒加载。
     */
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 秒杀下单 —— 第一层：只碰 Redis，不碰数据库。
     *
     * 三件事（判断库存、判断一人一单、预扣库存）全部在 Lua 脚本里原子完成，
     * 完成后立刻把订单扔进阻塞队列返回，不等数据库。
     */
    @Override
    public Result killVoucher(Long voucherId) {
        // 【修复】UserHolder.getUser() 可能是 null（未登录），先取出来判空再解引用，
        // 否则就是 NullPointerException，被全局异常处理器吞成"服务器异常"。
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("未登录，请先登录后再抢购");
        }
        Long userId = currentUser.getId();

        // 1. 执行 Lua 脚本：一次网络往返，原子完成"库存 + 一人一单"判断并预扣库存
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());

        // 【修复】脚本返回 null 说明执行异常，不能直接 result.intValue()，会 NPE
        int r = result == null ? -1 : result.intValue();

        // 2. 不为 0 说明没有资格，按脚本返回值给出能定位问题的提示
        if (r != 0) {
            if (r == 1) {
                return Result.fail("库存不足");
            }
            if (r == 2) {
                return Result.fail("不能重复下单");
            }
            if (r == 3) {
                return Result.fail("该优惠券不是秒杀券或活动不存在");
            }
            if (r == 4) {
                // 脚本里 stock 的值不是合法数字，说明 Redis 库存被写脏了
                // （典型场景：同步数据时混进了 \r，导致 incrby 报
                //   ERR value is not an integer or out of range）
                return Result.fail("秒杀库存数据异常，请重新同步 Redis 库存（seckill:stock:" + voucherId + "）");
            }
            return Result.fail("秒杀失败，请稍后重试");
        }

        // 3. 为 0：有购买资格，组装订单信息丢进阻塞队列，由后台线程去写数据库
        long orderId = reidsWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        // 队列容量远超实际并发，add 不会阻塞
        orderTasks.add(voucherOrder);

        // 4. 立刻返回订单id，不等数据库写入完成
        return Result.ok(orderId);
    }

    /**
     * 队列消费线程：不停地从阻塞队列里取订单，然后落库。
     */
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // take() 是阻塞的：队列为空时就在这里等着，不占用 CPU
                    VoucherOrder voucherOrder = orderTasks.take();
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    // 被中断说明应用正在关闭，恢复中断标记后退出循环
                    Thread.currentThread().interrupt();
                    log.warn("秒杀订单处理线程被中断，退出");
                    return;
                } catch (Exception e) {
                    // ★ 单条订单处理失败绝不能把整个线程搞死，
                    //   否则队列从此再也没人消费，后面所有订单都会"静默丢失"。
                    log.error("处理秒杀订单异常", e);
                }
            }
        }
    }

    /**
     * 处理单条订单：加分布式锁 → 调用事务方法落库。
     *
     * 注意这里是线程池的线程，不是处理 HTTP 请求的线程，
     * UserHolder（ThreadLocal）在这里取不到值，用户id只能从 voucherOrder 里拿。
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();

        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 不传参数的 tryLock：抢不到立刻返回 false，且不会抛受检异常
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("不允许重复下单，orderId={}", voucherOrder.getId());
            return;
        }
        try {
            selfProxy().createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 秒杀下单 —— 第二层：真正写数据库。
     *
     * 这个方法里的三件事（一人一单判断、扣库存、建订单）必须是一个整体：
     * 任何一步失败，整段都要回滚，不能出现"订单没建成、库存却少了"的情况。
     *
     * 【注意】这个方法只有在被"代理对象"调用时，@Transactional 才会生效。
     */
    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        // 数据库层再查一次一人一单，作为最后一道防线
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            log.error("用户已购买过，userId={}，voucherId={}", userId, voucherId);
            return;
        }

        // 扣减库存 —— 乐观锁：把"库存>0"塞进 SQL 的 WHERE 里，靠数据库行锁保证不超卖
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足，voucherId={}", voucherId);
            return;
        }

        // 创建订单
        save(voucherOrder);
    }
}
