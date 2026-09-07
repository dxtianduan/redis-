package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        //if判断
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);

        //验证码保存在session
session.setAttribute("code",code);
        //发送验证码给客户
log.debug("发送短信成功：{}",code);
        //返回OK
        return Result.ok();
    }


    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号和验证码,分别校验
        //1手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //2验证码
        Object code = session.getAttribute("code");
        String code1 = loginForm.getCode();
        if (code==null||!code.toString().equals(code1)) {
            //不一致
            return Result.fail("验证码错误");
        }
        //一致，，判断查询用户
        User user = query().eq("phone", phone).one();
        //判断用户存在
        if (user==null) {
            user=creatUserWithPhone(phone);
        }
        //保存到session中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        session.setAttribute("user", userDTO);
        return Result.ok();
    }

    private User creatUserWithPhone(String phone) {
        //创建用户
        User user = new User();
user.setPhone(phone);
user.setNickName("user_"+RandomUtil.randomString(10));
//保存用户
        save(user);
        return user;
    }
}
