package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RefreshTokenInterCeptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterCeptor(StringRedisTemplate stringRedisTemplate) {
this.stringRedisTemplate=stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        //HttpSession session = request.getSession();

        //
         //
         //
         //
        //获取请求头中的token
        String token = request.getHeader("authorization");

        //判断token是否为空
        if (StrUtil.isBlank(token)) {
            return true;
        }

        //获取session中的用户
        //Object user = session.getAttribute("user");

        //基于token获取Redis中的用户
        Map<Object, Object> userMapObj = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
// key强转String
        Map<String, String> userMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : userMapObj.entrySet()) {
            String key = entry.getKey().toString();
            // null 会转为字符串 "null"，不会抛异常
            String value = String.valueOf(entry.getValue());
            userMap.put(key, value);
        }


        //判断用户是否存在
        //不存在，拦截
//        if (user==null){
//           response.setStatus(401);
//         return false;
//      }
// 1. 打印拿到的 token
        log.debug("拦截器拿到的token：{}", token);

// 2. 打印拼接后的完整 Redis key
        String key = RedisConstants.LOGIN_USER_KEY + token;
        log.debug("拼接的Redis key：{}", key);


        //不存在，拦截
        if (userMap.isEmpty()) {
            return true;
        }

        //将查询到的hash数据转化为UserDtO
       UserDTO userDTO = BeanUtil.toBean(userMap, UserDTO.class);

        //保存用户ID到threadlocal
//UserHolder.saveUser((UserDTO) user);
//保存用户ID到threadlocal
UserHolder.saveUser(userDTO);

        //解析token有效期
stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
