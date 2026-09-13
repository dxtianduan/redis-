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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户详情实体，对应表 tb_user_info
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_info")
@ApiModel(value = "UserInfo 用户详情", description = "用户的扩展资料，如城市、简介、粉丝数")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，用户id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    @ApiModelProperty(value = "用户id，与 tb_user 的 id 一致", example = "1")
    private Long userId;

    /**
     * 城市名称
     */
    @ApiModelProperty(value = "所在城市", example = "杭州")
    private String city;

    /**
     * 个人介绍，不要超过128个字符
     */
    @ApiModelProperty(value = "个人介绍，不超过128个字")
    private String introduce;

    /**
     * 粉丝数量
     */
    @ApiModelProperty(value = "粉丝数", hidden = true)
    private Integer fans;

    /**
     * 关注的人的数量
     */
    @ApiModelProperty(value = "关注数", hidden = true)
    private Integer followee;

    /**
     * 性别，0：男，1：女
     */
    @ApiModelProperty(value = "性别：0男，1女", example = "0")
    private Boolean gender;

    /**
     * 生日
     */
    @ApiModelProperty(value = "生日", example = "2000-01-01")
    private LocalDate birthday;

    /**
     * 积分
     */
    @ApiModelProperty(value = "积分", hidden = true)
    private Integer credits;

    /**
     * 会员级别，0~9级,0代表未开通会员
     */
    @ApiModelProperty(value = "会员等级：0未开通，1~9级", hidden = true)
    private Boolean level;

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
