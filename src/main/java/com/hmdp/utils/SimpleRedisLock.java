package com.hmdp.utils;


import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements lLock{
   private String name;
   private final static  String key_="lock:";
   private static final String id_=UUID.randomUUID().toString(true)+"-";

private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取当前线程ID
        String id =id_+ Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key_ + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //获取线程标志
String threadID=id_+Thread.currentThread().getId();
        //获取锁中的标志
        String string = stringRedisTemplate.opsForValue().get(key_ + name);
        //判断是否一致
if (threadID.equals(string)){
        //释放锁
stringRedisTemplate.delete(key_+name);}
    }
}
