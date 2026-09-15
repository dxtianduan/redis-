package com.hmdp.utils;


import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.validation.Valid;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements lLock {
    private String name;
    private final static String key_ = "lock:";
    private static final String id_ = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> DEFAULT_REDIS_SCRIPT;

    static {
        DEFAULT_REDIS_SCRIPT = new DefaultRedisScript<>();
        DEFAULT_REDIS_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        DEFAULT_REDIS_SCRIPT.setResultType(Long.class);
    }


    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取当前线程ID
        String id = id_ + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key_ + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }


    //Lua实现

    public void unlock() {
        //调用Lua脚本
        stringRedisTemplate
                .execute(
                        DEFAULT_REDIS_SCRIPT,
                        Collections.singletonList(key_ + name),
                        id_ + Thread.currentThread().getId());
    }
}

//    public void unlock() {
//        //获取线程标志
//String threadID=id_+Thread.currentThread().getId();
//        //获取锁中的标志
//        String string = stringRedisTemplate.opsForValue().get(key_ + name);
//        //判断是否一致
//if (threadID.equals(string)){
//        //释放锁
//stringRedisTemplate.delete(key_+name);}
//    }
//}

//}
