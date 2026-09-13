package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 优惠券相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher")
@Api(tags = "04-优惠券模块")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增普通券
     *
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping
    @ApiOperation(value = "新增普通优惠券", notes = "普通券没有库存和起止时间的概念，type 填 0。返回新建的券id。")
    public Result addVoucher(
            @ApiParam(value = "优惠券信息", required = true)
            @RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增秒杀券
     *
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("seckill")
    @ApiOperation(value = "新增秒杀券", notes = "一次请求会往两张表写：tb_voucher（券基本信息）+ tb_seckill_voucher（库存和起止时间）。所以请求体要同时带上 stock、beginTime、endTime，type 填 1。时间格式形如 2026-01-01T10:00:00。")
    public Result addSeckillVoucher(
            @ApiParam(value = "优惠券信息，含秒杀库存与起止时间", required = true)
            @RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     *
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    @ApiOperation(value = "查询店铺的优惠券列表", notes = "不用登录。返回该店铺下所有券，列表里的 id 就是秒杀接口要传的 voucherId。")
    public Result queryVoucherOfShop(
            @ApiParam(value = "店铺id", required = true, example = "1")
            @PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }
}
