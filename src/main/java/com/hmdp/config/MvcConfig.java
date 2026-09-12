package com.hmdp.config;

import com.hmdp.utils.LoginInterCeptor;
import com.hmdp.utils.RefreshTokenInterCeptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
@Resource
private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
registry.addInterceptor(new LoginInterCeptor(stringRedisTemplate))
        .excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources/**",
                "/v2/api-docs",
                "/favicon.ico"
        ).order(1);
registry.addInterceptor(new RefreshTokenInterCeptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
        WebMvcConfigurer.super.addInterceptors(registry);
    }

    }

