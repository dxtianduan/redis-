package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Redissionclient {

    /**
     * 创建 RedissonClient，交给 Spring 管理。
     * 方法名 redissonClient() 就是 Bean 的名字，以后要注入就用这个名字对应的类型。
     */
    @Bean
    public RedissonClient redissonClient() {   // ← 这里必须有 ()，@Bean 方法不能带参数（有参数会被当成要注入的依赖）
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        // 创建 redissonClient 对象
        return Redisson.create(config);
    }
}
