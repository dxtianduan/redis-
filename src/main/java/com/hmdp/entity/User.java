package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户实体，对应表 tb_user
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
@ApiModel(value = "User 用户", description = "登录用户的基本信息")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "用户id", example = "1")
    private Long id;

    /**
     * 手机号码
     */
    @ApiModelProperty(value = "手机号", example = "13612345678")
    private String phone;

    /**
     * 密码，加密存储
     */
    @ApiModelProperty(value = "密码（加密存储）", hidden = true)
    private String password;

    /**
     * 昵称，默认是随机字符
     */
    @ApiModelProperty(value = "昵称", example = "user_abc123")
    private String nickName;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "头像地址")
    private String icon = "";

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updateTime;


}
