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
 * 商铺实体，对应表 tb_shop
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")
@ApiModel(value = "Shop 商铺", description = "商铺信息")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "商铺id，新增时不用填", example = "1")
    private Long id;

    /**
     * 商铺名称
     */
    @ApiModelProperty(value = "商铺名称", example = "102茶餐厅")
    private String name;

    /**
     * 商铺类型的id
     */
    @ApiModelProperty(value = "商铺类型id，对应 /shop-type/list 返回的 id", example = "1")
    private Long typeId;

    /**
     * 商铺图片，多个图片以','隔开
     */
    @ApiModelProperty(value = "商铺图片地址，多张图用英文逗号隔开")
    private String images;

    /**
     * 商圈，例如陆家嘴
     */
    @ApiModelProperty(value = "所在商圈", example = "陆家嘴")
    private String area;

    /**
     * 地址
     */
    @ApiModelProperty(value = "详细地址")
    private String address;

    /**
     * 经度
     */
    @ApiModelProperty(value = "经度")
    private Double x;

    /**
     * 维度
     */
    @ApiModelProperty(value = "纬度")
    private Double y;

    /**
     * 均价，取整数
     */
    @ApiModelProperty(value = "人均价格，单位元，取整数", example = "80")
    private Long avgPrice;

    /**
     * 销量
     */
    @ApiModelProperty(value = "销量")
    private Integer sold;

    /**
     * 评论数量
     */
    @ApiModelProperty(value = "评论数量")
    private Integer comments;

    /**
     * 评分，1~5分，乘10保存，避免小数
     */
    @ApiModelProperty(value = "评分，1~5分乘以10保存，所以显示时要除以10", example = "45")
    private Integer score;

    /**
     * 营业时间，例如 10:00-22:00
     */
    @ApiModelProperty(value = "营业时间", example = "10:00-22:00")
    private String openHours;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


    @TableField(exist = false)
    @ApiModelProperty(value = "距离，非数据库字段，由前端传入坐标后计算")
    private Double distance;
}
