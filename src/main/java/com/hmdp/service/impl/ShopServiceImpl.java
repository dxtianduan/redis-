package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheCllent;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
@Resource
private CacheCllent cacheCllent;
    @Override
    public Result querById(Long id) {
        //缓存穿透
        //Shop shop = QuerWithPassThrough(id);
        //方法类，缓存穿透
        Shop shop=cacheCllent.QuerWithPassThrough("cache:shop:",id,Shop.class,id2->getById(id),30L,TimeUnit.MINUTES);

        //缓存击穿
       // Shop shop1 = QuerWithMutex(id);
       // Shop shop1=cacheCllent.QuerWithLogicalExpire("cache:shop:",id,Shop.class,id2->getById(id),30L,TimeUnit.MINUTES);
//逻辑过期
      //  Shop shop2 = QuerWithLogicalExpire(id);
        return Result.ok(shop);
    }

    private boolean tryLock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

//    public void saveShop2Redis(Long id, Long expireSeconds) {
//        //查询店铺数据
//        Shop shop = getById(id);
//        //封装；逻辑到期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //写入Redis\
//        stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(redisData));
//    }

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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        String key="shop:geo:"+typeId;
        //判断是否需要根据坐标查询
if(x==null||y==null){
    //不需要坐标查询，使用数据库查询
    Page<Shop> page = query()
            .eq("type_id", typeId)
            .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
    return Result.ok(page.getRecords());
}
        //计算分页参数
int from=(current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
int end=current*SystemConstants.DEFAULT_PAGE_SIZE;
        //查询Redis，按照距离排序，分页、、得到shopid和distance
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(key, GeoReference.fromCoordinate(x, y), new Distance(5000)
                , RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().sortAscending().includeDistance().limit(15));
        if (results==null){
            return Result.ok(Collections.emptyList());
        }
        //解析出id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.size()<=from){
            //没有下一页
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(content.size());
        Map<String,Distance>distanceMap=new HashMap<>(list().size());
        //截取from到end部分
        content.stream().skip(from).forEach(result->{
            String name = result.getContent().getName();
            ids.add(Long.valueOf(name));
            //距离
            Distance distance = result.getDistance();
            distanceMap.put(name,distance);
        });
        //根据id查询店铺
        String join = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by field(id," + join + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    //Redis缓存穿透
//    public Shop QuerWithPassThrough(Long id) {
//        //从Redis查询
//        String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//        //判断存在
//        if (StrUtil.isNotBlank(string)) {
//            //存在，直接返回
//            return JSONUtil.toBean(string, Shop.class);
//        }
//        if (string != null) {
//            return null;
//        }
//        //不存在，根据ID查询数据库
//        Shop shop = getById(id);
//        //不存在，返回错误
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set("cache:shop:" + id, null, 3, TimeUnit.MINUTES);
//            return null;
//        }
//        //存在，写入Redis
//        stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);
//        //返回
//        return shop;
//    }

//    private static final ExecutorService CACHE = Executors.newFixedThreadPool(10);


//    //逻辑过期Redis处理，缓存击穿
//    public Shop QuerWithLogicalExpire(Long id) {
//        //从Redis查询
//        String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//        //判断存在
//        if (StrUtil.isBlank(string)) {
//            //不存在，直接返回
//            return null;
//        }
//        //命中，先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(string, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop1 = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            //未过期，返回信息
//            return shop1;
//        }
//        //过期，需要缓存重建
//        //获取互斥锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean tryLock = tryLock(lockKey);
//        //判断获取锁成功
//        if (!tryLock) {
//            //开启独立线程,使用线程池
//            CACHE.submit(() -> {
//                try {
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unLock(lockKey);
//                }
//
//            });
//        }
//        return shop1;
//    }

//redis缓存击穿
            public Shop QuerWithMutex (Long id){
                //从Redis查询
                String string = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
                //判断存在
                if (StrUtil.isNotBlank(string)) {
                    //存在，直接返回
                    return JSONUtil.toBean(string, Shop.class);
                }
                if (string != null) {
                    return null;
                }
                //缓存击穿实现缓存重建
                //实现缓存重建
                //获取互斥锁
                String lockKey = "lock:shop:" + id;
                try {
                    boolean tryLock = tryLock(lockKey);
                    //失败，失眠并重试
                    if (!tryLock) {
                        try {
                            Thread.sleep(50);
                            return QuerWithMutex(id);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    //判断存在
                    if (StrUtil.isNotBlank(string)) {
                        //存在，直接返回
                        return JSONUtil.toBean(string, Shop.class);
                    }
                    //不存在，根据ID查询数据库
                    Shop shop = getById(id);
                    //不存在，返回错误
                    if (shop == null) {
                        stringRedisTemplate.opsForValue().set("cache:shop:" + id, null, 3, TimeUnit.MINUTES);
                        return null;
                    }

                    //返回
                    return shop;
                } catch (RuntimeException e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放互斥锁
                    unLock(lockKey);

                }
            }
        }

