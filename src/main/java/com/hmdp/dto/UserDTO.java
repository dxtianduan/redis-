package com.hmdp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "UserDTO 登录用户", description = "只含公开信息，不含手机号和密码")
@Data
public class UserDTO {
    @ApiModelProperty(value = "用户id", example = "1")
    private Long id;

    @ApiModelProperty(value = "昵称", example = "鱼皮")
    private String nickName;

    @ApiModelProperty(value = "头像地址")
    private String icon;
}
