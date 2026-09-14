package com.hmdp.controller;


import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 用户相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "01-用户模块")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    @ApiOperation(value = "发送短信验证码", notes = "验证码不会返回给前端，而是打印在 IDEA 控制台（日志里的『发送短信成功』）。测试时去控制台抄，然后传给登录接口。")
    public Result sendCode(
            @ApiParam(value = "手机号，11位", required = true, example = "13456789011")
            @RequestParam("phone") String phone,
            // @ApiIgnore：HttpSession 是 Servlet 容器注入的对象，不是接口参数。
            // 不加这个注解，Swagger 会把它当请求参数展开，文档上就会多出
            // creationTime / id / lastAccessedTime / maxInactiveInterval / new / valueNames 这 6 个假参数，
            // 看着像要你填，其实一个都不用填（填了也没用）。
            @ApiIgnore HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    @ApiOperation(value = "登录", notes = "★ 必须先调『发送短信验证码』拿到验证码，验证码在 IDEA 控制台日志里（搜『发送短信成功』），有效期 2 分钟。登录成功后返回 token，把它填到文档的『全局参数』里（参数名 authorization，类型 header），其他接口就会自动带上。")
    public Result login(
            @ApiParam(value = "登录参数，手机号必填；验证码和密码二选一", required = true)
            @RequestBody LoginFormDTO loginForm,
            @ApiIgnore HttpSession session) {
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    @ApiOperation(value = "登出", notes = "课程中此功能未实现，调用会返回『功能未完成』。")
    public Result logout() {
        return Result.fail("功能未完成");
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/me")
    @ApiOperation(value = "获取当前登录用户", notes = "需要带 token。token 无效或过期时会返回 401。")
    public Result me() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 查询指定用户详情
     */
    @GetMapping("/info/{id}")
    @ApiOperation(value = "查询用户详情", notes = "返回城市、简介、生日等信息。如果该用户还没填过详情，会返回 success=true 但 data 为空。")
    public Result info(
            @ApiParam(value = "用户id", required = true, example = "1")
            @PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }
}
