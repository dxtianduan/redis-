package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;

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
@Resource
private StringRedisTemplate stringRedisTemplate;
    public Result sendCode(String phone, HttpSession session) {

        //校验手机号
        //if判断
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);

        //验证码保存在session
//session.setAttribute("phone",phone);
        //验证码保存在Redis当中
        //★ 这里是"验证码存哪儿"的关键：key = login:code:手机号，value = 6位数字，TTL = LOGIN_CODE_TTL 分钟
        //  过期后 key 会被 Redis 自动删除，再登录就会取不到值（那才是真正的"验证码错误"来源）
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码给客户
        //★ 练习项目不会真发短信，验证码也不会在接口返回值里给前端，只打印在控制台。
        //  用 info 而不是 debug：万一哪天日志级别调成 info，debug 就看不到了，找起来更费劲。
        log.info("★ 短信验证码已生成 -> 验证码：{}（手机号：{}，{} 分钟内有效，Redis key：{}{}）",
                code, phone, LOGIN_CODE_TTL, LOGIN_CODE_KEY, phone);
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
        //2sessoin中获取验证码
       // Object code = session.getAttribute("code");


        //Redis中获取验证码
        String code= stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code1 = loginForm.getCode();
        //★ 下面三个 if 是这次的重点：原来只有一句"验证码错误"，把三种完全不同的原因混在了一起：
        //   ① 没调 /user/code      -> Redis 里没这个 key
        //   ② 调了但超过 10 分钟   -> key 已过期被删
        //   ③ 手机号写错/验证码敲错 -> key 有值但对不上
        //  分开提示后，一看错误信息就知道该去查哪一步，不用再猜。
        if (code == null) {
            return Result.fail("验证码不存在或已过期（有效期 " + LOGIN_CODE_TTL
                    + " 分钟）。请先调用『发送短信验证码』，并确认使用的手机号完全一致：" + phone);
        }
        if (code1 == null || code1.trim().isEmpty()) {
            return Result.fail("请求体里没有验证码：body 要写成 {\"phone\":\"手机号\",\"code\":\"6位数字\"} 两个字段都传");
        }
        if (!code.equals(code1.trim())) {
            //不一致
            return Result.fail("验证码错误：你传的是 " + code1 + "，Redis 里存的是另一个值（注意别抄错、别抄了上一次的）");
        }
        //一致，，判断查询用户
        User user = query().eq("phone", phone).one();
        //判断用户存在
        if (user==null) {
            user=creatUserWithPhone(phone);
        }
        //保存到session中
        //UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //session.setAttribute("user", userDTO);
        //保存用户到Redis中
        //1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();

        //2.将user对象作为hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValve)->fieldValve.toString()));
        //存储
        String tokenKey ="login:token:"+token;
stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//设置token有效期
stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);
return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取当前用户
        Long usereId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keysuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key="sign:"+usereId+keysuffix;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //写入Redis
stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取当前用户
        Long usereId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keysuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key="sign:"+usereId+keysuffix;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //获取本月截止今天所有签到记录,返回十进制数字
        BitFieldSubCommands result = BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0);
        List<Long> longs = stringRedisTemplate.opsForValue().bitField(
                key, result
        );
        if (longs==null||longs.isEmpty()){
            return Result.ok(0);
        }
        Long num=longs.get(0);
        if (num==null||num==0){
            return Result.ok(0);
        }
        //循环遍历
        int count=0;
while (true){
    //让数字与一做与运算
    if ((num&1)==0) {
        //为0,说明未签到
        break;
    }else {
        //为一，已签到，计数器加一
count++;
    }
    //把数字右移一位，抛弃最后一个bit位
    num>>>=1;
}

        return Result.ok(count);
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
