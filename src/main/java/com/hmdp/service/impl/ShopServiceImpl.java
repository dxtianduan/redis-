package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

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
        //缓存穿透
        Shop shop=QuerWithPassThrough(id);
        //缓存击穿
Shop shop1=QuerWithMutex(id);
  return Result.ok(shop);
    }
private boolean tryLock(String key){
    Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(b);
}
private void unLock(String key){
        stringRedisTemplate.delete(key);
}

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺ID为空");
        }
updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + id);

        return Result.ok(shop);
    }
    //Redis缓存穿透
public Shop QuerWithPassThrough(Long id){
    //从Redis查询
    String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
    //判断存在
    if (StrUtil.isNotBlank(string)){
        //存在，直接返回

        return JSONUtil.toBean(string, Shop.class);
    }
    if (string!=null) {
        return null;
    }
    //不存在，根据ID查询数据库
    Shop shop = getById(id);
    //不存在，返回错误
    if (shop==null) {
        stringRedisTemplate.opsForValue().set("cache:shop:" + id,null,3,TimeUnit.MINUTES);
        return null;
    }
    //存在，写入Redis
    stringRedisTemplate.opsForValue().set("cache:shop:" + id,JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);
    //返回
    return shop;
}

//redis缓存击穿
    public Shop QuerWithMutex(Long id){
        //从Redis查询
        String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        //判断存在
        if (StrUtil.isNotBlank(string)){
            //存在，直接返回

            return JSONUtil.toBean(string, Shop.class);
        }
        if (string!=null) {
            return null;
        }
        //缓存击穿实现缓存重建
        //实现缓存重建
        //获取互斥锁
        String lockKey="lock:shop:"+id;
        try {
            boolean tryLock = tryLock(lockKey);
            //失败，失眠并重试
            if (!tryLock)
            {
                try {
                    Thread.sleep(50);
                   return QuerWithMutex(id);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
            //判断存在
            if (StrUtil.isNotBlank(string)){
                //存在，直接返回

                return JSONUtil.toBean(string, Shop.class);
            }
            //不存在，根据ID查询数据库
            Shop shop = getById(id);
            //不存在，返回错误
            if (shop==null) {
                stringRedisTemplate.opsForValue().set("cache:shop:" + id,null,3,TimeUnit.MINUTES);
                return null;
            }
            //存在，写入Redis
            stringRedisTemplate.opsForValue().set("cache:shop:" + id,JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);

            //返回
            return shop;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }finally {
            //释放互斥锁
            unLock(lockKey);

        }
    }
}
