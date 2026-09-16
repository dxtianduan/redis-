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
        // ★【本次修复】进来第一件事：清掉线程上残存的用户身份。
        //
        // 为什么必须清？Tomcat 是用"线程池"处理请求的，同一个线程会被成百上千次复用，
        // 而 UserHolder 底下的 ThreadLocal 是"绑在线程上"的：
        //   请求A（用户5）跑完 → 如果没清理，用户5 就一直挂在这个线程上
        //   → 请求B（任何人不带 token）恰好落到同一个线程
        //   → LoginInterCeptor 看到 UserHolder 不为空，直接放行
        // 结果有两个，都很严重：
        //   1. 认证形同虚设：不传 token 也能调用需要登录的接口；
        //   2. 用户串号：B 的操作会记到 A 头上（例如错误地替别人下单）。
        // 所以不管是"带 token 但 token 已失效"，还是"压根没带 token"，
        // 都必须先把残留值清干净，再走后面的逻辑。
        UserHolder.removeUser();

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
        // ★【本次修复】请求结束时清理 ThreadLocal，把线程"洗干净"再还给线程池。
        //
        // 原来的代码只调用了父类的空实现，等于从来不清理。
        // preHandle 里清一次是"防上一个请求的残留"，这里再清一次是"不给下一个请求留残留"，
        // 两头都堵住才算完整。
        //
        // 补充一个知识点：afterCompletion 的执行顺序和 preHandle 相反。
        // 本拦截器 order=0（最外层），所以它的 afterCompletion 会最后执行，
        // 正好在所有业务逻辑都跑完之后统一清理，位置是合适的。
        UserHolder.removeUser();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
