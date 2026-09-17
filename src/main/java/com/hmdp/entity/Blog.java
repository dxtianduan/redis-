package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 探店笔记实体，对应表 tb_blog
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")
@ApiModel(value = "Blog 探店笔记", description = "用户发布的探店图文")
public class Blog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "笔记id，新增时不用填", example = "1")
    private Long id;
    /**
     * 商户id
     */
    @ApiModelProperty(value = "关联的商铺id", example = "1")
    private Long shopId;
    /**
     * 用户id
     */
    @ApiModelProperty(value = "发布者用户id，新增时不用填（后端从登录态取）", example = "1")
    private Long userId;
    /**
     * 用户图标
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "发布者头像，返回时才有")
    private String icon;

    /**
     * 用户姓名
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "发布者昵称，返回时才有")
    private String name;
    /**
     * 是否点赞过了
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前登录用户是否已点赞")
    private Boolean isLike;

    /**
     * 标题
     */
    @ApiModelProperty(value = "笔记标题", example = "探店日记")
    private String title;

    /**
     * 探店的照片，最多9张，多张以","隔开
     */
    @ApiModelProperty(value = "探店照片地址，最多9张，用英文逗号隔开")
    private String images;

    /**
     * 探店的文字描述
     */
    @ApiModelProperty(value = "探店文字描述")
    private String content;

    /**
     * 点赞数量
     */
    @ApiModelProperty(value = "点赞数", hidden = true)
    private Integer liked;

    /**
     * 评论数量
     */
    @ApiModelProperty(value = "评论数", hidden = true)
    private Integer comments;

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
