package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 优惠券订单相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
@Api(tags = "05-优惠券订单模块")
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService iVoucherOrderService;

    @PostMapping("seckill/{id}")
    @ApiOperation(value = "秒杀下单", notes = "必须带 token（登录后才能抢）。成功返回订单id；失败会返回具体原因，如『库存不足』『秒杀尚未开始』。测试前请先确认 tb_seckill_voucher 里有数据，否则会报券不存在。")
    public Result seckillVoucher(
            @ApiParam(value = "优惠券id，来自『查询店铺的优惠券列表』", required = true, example = "1")
            @PathVariable("id") Long voucherId) {
        return iVoucherOrderService.killVoucher(voucherId);
    }
}
