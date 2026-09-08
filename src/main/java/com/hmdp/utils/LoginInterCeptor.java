package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.handler.Handler;

public class LoginInterCeptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        //HttpSession session = request.getSession();

        //
         //
         //
         //
        //获取请求头中的token

        //判断token是否为空

        //获取session中的用户
        //Object user = session.getAttribute("user");

        //基于token获取Redis中的用户



        //判断用户是否存在
        //不存在，拦截
//        if (user==null){
//           response.setStatus(401);
//         return false;
//      }


        //不存在，拦截


        //将查询到的hash数据转化为UserDtO

        //保存用户ID到threadlocal
UserHolder.saveUser((UserDTO) user);


//保存用户ID到threadlocal


        //解析token有效期

        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
