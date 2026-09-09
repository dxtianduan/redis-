package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.handler.Handler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@Slf4j
public class LoginInterCeptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterCeptor(StringRedisTemplate stringRedisTemplate) {
this.stringRedisTemplate=stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       //判断是否需要拦截
        if(UserHolder.getUser()==null)
        {
            //没有就拦截
response.setStatus(401);
            //拦截
return false;
        }
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
