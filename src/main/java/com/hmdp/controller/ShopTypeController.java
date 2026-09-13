package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 商铺类型相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
@Api(tags = "02-店铺类型模块")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    @ApiOperation(value = "查询店铺类型列表", notes = "首页顶部的分类栏（美食、KTV、酒吧……）。这个接口不需要登录，也不用传任何参数。数据来自 Redis 缓存。")
    public Result queryTypeList() {
        List<ShopType> typeList = (List<ShopType>) typeService.queryTypeListByString();
        return Result.ok(typeList);
    }
}
