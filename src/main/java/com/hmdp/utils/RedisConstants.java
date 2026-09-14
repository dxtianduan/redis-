package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    /**
     * 验证码在 Redis 里的存活时间（单位：分钟，见 UserServiceImpl#sendCode 里的 TimeUnit.MINUTES）。
     * <p>
     * 课程原版是 2 分钟，对“手敲控制台验证码 -> 切到接口文档 -> 填参数 -> 点发送”这套手工流程太短了，
     * 稍微慢一点就会过期，然后登录一直报“验证码错误”，非常容易误判成代码 BUG，所以放宽到 10 分钟。
     * 生产环境一般 1~5 分钟，练习阶段长一点更省事。
     */
    public static final Long LOGIN_CODE_TTL = 10L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
