package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
@Autowired
private StringRedisTemplate stringRedisTemplate;
    public Object queryTypeListByString() {
        final String cache_key="cache:type:";
        String string = stringRedisTemplate.opsForValue().get(cache_key);
        if (StrUtil.isNotBlank(string)) {
            List<ShopType> list = JSONUtil.toList(string, ShopType.class);
            return Result.ok(list);
        }
        List<ShopType> typeList = this.query().orderByAsc("sort").list();
        if (typeList==null||typeList.isEmpty()) {
            return typeList;
        }
        stringRedisTemplate.opsForValue().set(
                cache_key
                ,JSONUtil.toJsonStr(typeList),30, TimeUnit.MINUTES
        );
        return typeList;
    }
}
