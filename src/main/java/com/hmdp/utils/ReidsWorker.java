package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.swing.text.DateFormatter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class ReidsWorker {
    private static final long beginTime=1640995200L;

    private StringRedisTemplate stringRedisTemplate;
    public ReidsWorker(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }
    public long nextId(String keypre){
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowsecond = now.toEpochSecond(ZoneOffset.UTC);
        long TimeStamp= nowsecond- beginTime;
        //生成序列号
        //获取当前日期,精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长
        long count=stringRedisTemplate.opsForValue().increment("icr:"+keypre+":"+date);
        //拼接并返回
        return TimeStamp<<32|count;
    }
    public static void main(String[] args){
        LocalDateTime localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = localDateTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second="+second);
    }
}
