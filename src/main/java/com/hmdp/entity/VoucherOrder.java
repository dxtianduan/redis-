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
 * 优惠券订单实体，对应表 tb_voucher_order
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
@ApiModel(value = "VoucherOrder 优惠券订单", description = "秒杀券与普通券共用一张订单表")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    @ApiModelProperty(value = "订单id，使用 Redis 全局唯一ID生成，不依赖自增", example = "1000000000001")
    private Long id;

    /**
     * 下单的用户id
     */
    @ApiModelProperty(value = "下单用户id", example = "1")
    private Long userId;

    /**
     * 购买的代金券id
     */
    @ApiModelProperty(value = "购买的优惠券id", example = "1")
    private Long voucherId;

    /**
     * 支付方式 1：余额支付；2：支付宝；3：微信
     */
    @ApiModelProperty(value = "支付方式：1余额，2支付宝，3微信", example = "1")
    private Integer payType;

    /**
     * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
     */
    @ApiModelProperty(value = "订单状态：1未支付，2已支付，3已核销，4已取消，5退款中，6已退款", example = "1")
    private Integer status;

    /**
     * 下单时间
     */
    @ApiModelProperty(value = "下单时间", hidden = true)
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    @ApiModelProperty(value = "支付时间", hidden = true)
    private LocalDateTime payTime;

    /**
     * 核销时间
     */
    @ApiModelProperty(value = "核销时间", hidden = true)
    private LocalDateTime useTime;

    /**
     * 退款时间
     */
    @ApiModelProperty(value = "退款时间", hidden = true)
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updateTime;


}
