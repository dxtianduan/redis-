package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
@Resource
private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result querById(Long id) {
        //从Redis查询
        String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        //判断存在
        if (StrUtil.isNotBlank(string)){
            //存在，直接返回
            Shop shop = JSONUtil.toBean(string, Shop.class);
            return Result.ok(shop);
        }

        //不存在，根据ID查询数据库
        Shop shop = getById(id);
        //不存在，返回错误
        if (shop==null) {
            return Result.fail("用户不存在");
        }
        //存在，写入Redis
   stringRedisTemplate.opsForValue().set("cache:shop:" + id,JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);
        //返回
        return Result.ok(shop);
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺ID为空");
        }
updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + id);

        return Result
                .ok();
    }
}
