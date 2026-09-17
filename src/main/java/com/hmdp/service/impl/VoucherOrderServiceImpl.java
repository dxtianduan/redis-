package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    /**
     * 【本次修复1】Redis Stream 的名字，必须和 seckill.lua 里 xadd 的 key 一字不差。
     * 原来 Java 这边写的是 "streams.orders"（多了个 s），Lua 写的是 "stream.orders"，
     * 结果消息全写进了 A 房间，后台线程却蹲在 B 房间等，订单永远不会被消费。
     */
    private static final String QUEUE_NAME = "stream.orders";
    /** 消费者组名（一组消费者共享同一个消费进度） */
    private static final String GROUP_NAME = "g1";
    /** 消费者名（组里可以有多个消费者，这里只有 1 个后台线程） */
    private static final String CONSUMER_NAME = "c1";
    /** XREADGROUP 的阻塞时长：没消息就在这里等 2 秒，避免空转把 CPU 打满 */
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(2);

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
        // 这里必须指向 seckill.lua。
        // 原来写的是 unlock.lua —— 那是"释放锁"的脚本，被当成秒杀脚本执行，
        // 脚本里读 KEYS[1] 时拿不到值，Redis 直接抛错，
        // 异常一路冒到 WebExceptionAdvice，前端看到的就是"服务器异常"。
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 消费消息的线程池。
     *
     * ★ 核心线程数必须只有 1 个。
     *   如果开多个线程一起落库，"一人一单"的判断和库存扣减就会重新出现并发竞态，
     *   等于白折腾一趟 Redis 预判断。串行处理虽然慢，但天然不会有竞态。
     * ★ 线程要有名字：出问题时用 jstack 能一眼认出是哪个线程卡住了。
     * ★ 守护线程：主程序退出时不会因为它在 while 里而无法结束。
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
     * 在 @PostConstruct 里调用 AopContext.currentProxy() 会抛异常，应用直接启动失败：
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

    /**
     * 【本次修复2】后台消费线程的开关。
     * 应用关闭时先在 @PreDestroy 里把它置为 false，循环才能退出来，
     * 不会出现"Spring 已经销毁 Redis 连接、线程还在发请求"的报错。
     */
    private volatile boolean running = true;

    @Override
    public Result killVoucher(Long voucherId) {
        // UserHolder.getUser() 可能是 null（未登录），先取出来判空再解引用，
        // 否则就是 NullPointerException，被全局异常处理器吞成"服务器异常"。
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("未登录，请先登录后再抢购");
        }
        Long userId = currentUser.getId();
        long orderId = reidsWorker.nextId("order");

        // 1. 执行 Lua 脚本：一次网络往返，原子完成"库存 + 一人一单"判断、预扣库存，
        //    并把订单消息 xadd 进 stream.orders
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId));

        // 脚本返回 null 说明执行异常，不能直接 result.intValue()，会 NPE
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

        // 3. 为 0：有购买资格。订单消息已经由 Lua 脚本 xadd 进了 stream.orders，
        //    后台线程会自动把它落库，这里直接返回订单 id 即可，不等数据库。
        return Result.ok(orderId);
    }

    private IVoucherOrderService selfProxy() {
        IVoucherOrderService p = proxy;
        if (p == null) {
            p = applicationContext.getBean(IVoucherOrderService.class);
            proxy = p;
        }
        return p;
    }

    /**
     * 启动时干两件事：保证消费者组存在 → 把消费线程拉起来。
     */
    @PostConstruct
    private void init() {
        ensureConsumerGroup();
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 【本次修复2】应用关闭时先把消费线程停掉，再让 Spring 去销毁 Redis 连接。
     */
    @PreDestroy
    private void stop() {
        running = false;
        SECKILL_ORDER_EXECUTOR.shutdownNow();
        log.info("秒杀订单消费线程已停止");
    }

    /**
     * 【本次修复3】确保消费者组存在。
     *
     * XREADGROUP 要求"组"必须先存在，否则 Redis 直接报 NOGROUP：
     *   NOGROUP No such key 'stream.orders' or consumer group 'g1' in XREADGROUP with GROUP option
     * 于是线程会一直抛异常、一直重试，日志被刷屏。
     *
     * 用 XGROUP CREATE stream.orders g1 0 MKSTREAM：
     *   - 0      ：从消息队列头部开始消费（应用重启后不会漏掉积压的订单）
     *   - MKSTREAM：key 不存在时自动创建空 Stream
     * 组已存在时 Redis 返回 BUSYGROUP 错误，属于正常情况，忽略即可。
     */
    private void ensureConsumerGroup() {
        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.xGroupCreate(
                        QUEUE_NAME.getBytes(StandardCharsets.UTF_8),
                        GROUP_NAME,
                        ReadOffset.from("0"),
                        true);
                return null;
            });
            log.info("已创建消费者组 {} / {}", QUEUE_NAME, GROUP_NAME);
        } catch (Exception e) {
            log.info("消费者组 {} / {} 已存在，无需重复创建", QUEUE_NAME, GROUP_NAME);
        }
    }

    /**
     * 消息消费线程：不停地从 stream.orders 里读订单，然后落库。
     */
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (running) {
                try {
                    // 1. 读取消息队列中的订单信息
                    //    XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1).block(BLOCK_TIMEOUT),
                            StreamOffset.create(QUEUE_NAME, ReadOffset.lastConsumed())
                    );
                    // 2. 没有消息就继续下一轮循环
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    // 3. 解析消息（消息里的 userId / voucherId / id 反序列化成 VoucherOrder）
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);

                    // 4. 创建订单
                    handleVoucherOrder(voucherOrder);

                    // 5. ACK 确认：告诉 Redis 这条消息已经处理完了
                    stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
                } catch (Exception e) {
                    if (!running) {
                        // 应用正在关闭：Redis 连接已经被销毁，这里的异常不用管，直接退出
                        log.warn("应用关闭中，停止消费秒杀订单：{}", e.getMessage());
                        return;
                    }
                    log.error("订单处理异常", e);
                    // 处理失败的消息会留在 pending-list 里，去把它捞回来重试
                    handlePendingList();
                }
            }
            log.info("秒杀订单消费线程已退出");
        }

        /**
         * 处理 pending-list：上一条消息处理失败后，Redis 会把它记在"待确认列表"里，
         * 这里循环读取并重试，直到列表为空。
         */
        private void handlePendingList() {
            while (running) {
                try {
                    // XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream.orders 0
                    // 注意 offset 是 "0"：表示从 pending-list 的头部开始读
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(QUEUE_NAME, ReadOffset.from("0"))
                    );
                    // pending-list 没有消息，结束
                    if (list == null || list.isEmpty()) {
                        break;
                    }
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);

                    handleVoucherOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(QUEUE_NAME, GROUP_NAME, record.getId());
                } catch (Exception e) {
                    // 应用关闭/线程被中断：恢复中断标记并退出，不要把中断吞掉继续死循环
                    if (!running || Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        log.warn("秒杀订单消费线程被中断，退出");
                        return;
                    }
                    log.error("处理 pending-list 异常", e);
                    try {
                        // 睡 20ms 再重试，避免疯狂刷日志把磁盘写满
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("秒杀订单消费线程被中断，退出");
                        return;
                    }
                }
            }
        }
    }

    /**
     * 处理单条订单：加分布式锁 → 调用事务方法落库。
     * <p>
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
     * <p>
     * 这个方法里的三件事（一人一单判断、扣库存、建订单）必须是一个整体：
     * 任何一步失败，整段都要回滚，不能出现"订单没建成、库存却少了"的情况。
     * <p>
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
