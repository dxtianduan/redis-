package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 商铺相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
@Api(tags = "03-店铺模块")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询店铺详情", notes = "无需登录。内部走缓存：命中 Redis 直接返回，未命中则查库并写回缓存。")
    public Result queryShopById(
            @ApiParam(value = "商铺id", required = true, example = "1")
            @PathVariable("id") Long id) {
        return shopService.querById(id);
    }

    /**
     * 新增商铺信息
     *
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    @ApiOperation(value = "新增店铺", notes = "请求体只要填名称、类型id、商圈、地址这几项即可，id 交给数据库自增，返回新建的店铺id。")
    public Result saveShop(
            @ApiParam(value = "店铺信息", required = true)
            @RequestBody Shop shop) {
        shopService.save(shop);
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     *
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    @ApiOperation(value = "更新店铺", notes = "请求体里必须带 id，其余想改哪个字段就填哪个。成功后会同步清理 Redis 缓存。")
    public Result updateShop(
            @ApiParam(value = "店铺信息，必须带 id", required = true)
            @RequestBody Shop shop) {
        return shopService.update(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    @ApiOperation(value = "按类型分页查询店铺", notes = "不用登录。typeId 从『查询店铺类型列表』接口拿；页码从1开始，每页5条（SystemConstants 里写死了）。")
    public Result queryShopByType(
            @ApiParam(value = "商铺类型id", required = true, example = "1")
            @RequestParam("typeId") Integer typeId,
            @ApiParam(value = "页码，从1开始", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @ApiParam(value = "经度", example = "120.149993")
            @RequestParam(value = "x", required = false) Double x,
            @ApiParam(value = "纬度", example = "30.334229")
            @RequestParam(value = "y", required = false) Double y
    ) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     *
     * @param name    商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    @ApiOperation(value = "按名称模糊查询店铺", notes = "不用登录。name 不传就查全部；页码从1开始，每页上限10条。")
    public Result queryShopByName(
            @ApiParam(value = "商铺名称关键字，可留空", example = "茶餐厅")
            @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "页码，从1开始", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
}
