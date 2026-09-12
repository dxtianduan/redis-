package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.tokens.Token;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.yaml.snakeyaml.tokens.Token.ID.Key;

@Slf4j
@Component
public class CacheCllent {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheCllent(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;}

        public void set (String key, Object value, Long time, TimeUnit unit){
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
        }
    public void setWithlogicalExpire (String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    public <R,ID>R QuerWithPassThrough(String keyprefix, ID  id, Class<R>type, Function<ID,R>dbFallback,Long time, TimeUnit unit) {
        String key=keyprefix+id;
        //从Redis查询
        String string = stringRedisTemplate.opsForValue().get(key);
        //判断存在
        if (StrUtil.isNotBlank(string)) {
            //存在，直接返回
            return JSONUtil.toBean(string,type);
        }
        if (string != null) {
            return null;
        }
        //不存在，根据ID查询数据库
        R shop = dbFallback.apply(id);
        //不存在，返回错误
        if (shop == null) {
            stringRedisTemplate.opsForValue().set("cache:shop:" + id, null, 3, TimeUnit.MINUTES);
            return null;
        }
        //存在，写入Redis
        this.set(key,shop,time,unit);
        //返回
        return shop;
    }
    //逻辑过期Redis处理，缓存击穿
    public <R,ID>R  QuerWithLogicalExpire(String keypre, ID id, Class<R>type, Function<ID,R>dbfallabck,Long time, TimeUnit unit) {
        //从Redis查询
        String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        //判断存在
        if (StrUtil.isBlank(string)) {
            //不存在，直接返回
            return null;
        }
        //命中，先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(string, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R shop1 = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期，返回信息
            return shop1;
        }
        //过期，需要缓存重建
        //获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean tryLock = tryLock(lockKey);
        //判断获取锁成功
        if (tryLock) {
            //开启独立线程,使用线程池
            CACHE.submit(() -> {
                try {
                    R apply =dbfallabck.apply(id);
                    this.setWithlogicalExpire(keypre+id,apply,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }

            });
        }
        return shop1;
    }
    private boolean tryLock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    private static final ExecutorService CACHE = Executors.newFixedThreadPool(10);
}
