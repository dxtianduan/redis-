package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hmdp.utils.FlexibleLocalDateTimeDeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券实体，对应表 tb_voucher
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
@ApiModel(value = "Voucher 优惠券", description = "普通券与秒杀券共用，秒杀券多带库存和起止时间")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "优惠券id，新增时不用填", example = "1")
    private Long id;

    /**
     * 商铺id
     */
    @ApiModelProperty(value = "所属商铺id", example = "1")
    private Long shopId;

    /**
     * 代金券标题
     */
    @ApiModelProperty(value = "优惠券标题", example = "100元代金券")
    private String title;

    /**
     * 副标题
     */
    @ApiModelProperty(value = "副标题", example = "周一到周五可用")
    private String subTitle;

    /**
     * 使用规则
     */
    @ApiModelProperty(value = "使用规则说明", example = "全场通用，不与其他优惠叠加")
    private String rules;

    /**
     * 支付金额
     */
    @ApiModelProperty(value = "需要支付的金额，单位分", example = "8000")
    private Long payValue;

    /**
     * 抵扣金额
     */
    @ApiModelProperty(value = "可抵扣的金额，单位分", example = "10000")
    private Long actualValue;

    /**
     * 优惠券类型
     */
    @ApiModelProperty(value = "券类型：0普通券，1秒杀券", example = "0")
    private Integer type;

    /**
     * 优惠券类型
     */
    @ApiModelProperty(value = "状态：1上架，2下架，3过期", example = "1")
    private Integer status;
    /**
     * 库存
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "库存，仅秒杀券有；新增秒杀券时填这里", example = "100")
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "秒杀开始时间。两种写法都行：2026-01-01 10:00:00 或 2026-01-01T10:00:00", example = "2026-01-01 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "秒杀结束时间。两种写法都行：2026-01-01 12:00:00 或 2026-01-01T12:00:00", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", hidden = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", hidden = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


}
