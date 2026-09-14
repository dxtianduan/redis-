package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements lLock{
   private String name;
   private final static  String key_="lock:";
private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取当前线程ID
        long id = Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key_ + name, id + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //释放锁
stringRedisTemplate.delete(key_+name);
    }
}
