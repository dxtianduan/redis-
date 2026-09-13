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
        // 接口文档(knife4j/swagger)相关的路径必须完全放行，否则文档页面拿不到数据、显示为空
        String[] docPaths = {
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/swagger-resources/**",
                "/v2/api-docs",
                "/v2/api-docs/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/favicon.ico"
        };

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
                "/swagger-resources",
                "/swagger-resources/**",
                "/v2/api-docs",
                "/v2/api-docs/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/favicon.ico"
        ).order(1);

        // 刷新 token 的拦截器对所有路径生效，但文档路径要排除
        registry.addInterceptor(new RefreshTokenInterCeptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .excludePathPatterns(docPaths)
                .order(0);

        WebMvcConfigurer.super.addInterceptors(registry);
    }

    }

