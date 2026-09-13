package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 秒杀优惠券表，与优惠券是一对一关系
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_seckill_voucher")
@ApiModel(value = "SeckillVoucher 秒杀券", description = "秒杀券的库存与起止时间，对应表 tb_seckill_voucher")
public class SeckillVoucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的优惠券的id
     */
    @TableId(value = "voucher_id", type = IdType.INPUT)
    @ApiModelProperty(value = "关联的优惠券id", example = "1")
    private Long voucherId;

    /**
     * 库存
     */
    @ApiModelProperty(value = "秒杀库存", example = "100")
    private Integer stock;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createTime;

    /**
     * 生效时间
     */
    @ApiModelProperty(value = "秒杀开始时间。两种写法都行：2026-01-01 10:00:00 或 2026-01-01T10:00:00", example = "2026-01-01 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @ApiModelProperty(value = "秒杀结束时间。两种写法都行：2026-01-01 12:00:00 或 2026-01-01T12:00:00", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updateTime;


}
