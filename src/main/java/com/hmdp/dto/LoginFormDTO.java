package com.hmdp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "LoginFormDTO 登录表单", description = "登录请求参数：手机号+验证码，或者手机号+密码")
@Data
public class LoginFormDTO {
    @ApiModelProperty(value = "手机号，11位", required = true, example = "13612345678")
    private String phone;

    @ApiModelProperty(value = "短信验证码，只登录时用；验证码打在 IDEA 控制台里", example = "123456")
    private String code;

    @ApiModelProperty(value = "密码，走密码登录时才填")
    private String password;
}
