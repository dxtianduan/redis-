package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheCllent;
import com.hmdp.utils.ReidsWorker;
import io.lettuce.core.api.async.RedisGeoAsyncCommands;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.lang.annotation.Target;
import java.security.Key;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
   private StringRedisTemplate stringRedisTemplate;
@Resource
    private ShopServiceImpl shopService;
    @Resource
    private CacheCllent cacheCllent;
    @Resource
    private ReidsWorker reidsWorker;
    private ExecutorService executorService= Executors.newFixedThreadPool(500);
@Test
   public void testSaveShop (){
    CountDownLatch countDownLatch = new CountDownLatch(300);

    Runnable task=()->{
    for (int i=0;i<100;i++)
    {
        long id= reidsWorker.nextId("order");
        System.out.println("id="+id);
    }
    countDownLatch.countDown();
};
    long l = System.currentTimeMillis();

    for (int i = 0; i < 300; i++) {
executorService.submit(task);
    }
    try {
        countDownLatch.await();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    long l2 = System.currentTimeMillis();
    System.out.println(l2-l);
}
@Test
    void loadShopData(){
    //查询店铺信息
    List<Shop> list = shopService.list();
    //将店铺ID分组
    Map<Long,List<Shop>> map=list
            .stream()
            .collect(Collectors.groupingBy(shop ->shop.getTypeId()));
    //分批写入
    for (Map.Entry<Long, List<Shop>> longListEntry : map.entrySet()) {
        //获取类型ID
        Long typeId = longListEntry.getKey();
        String key="shop:geo:"+typeId;
        //获取同类型店铺集合
        List<Shop> value = longListEntry.getValue();
List<RedisGeoCommands.GeoLocation<String>>locations=new ArrayList<>(value.size());
        //写入经度纬度
        for (Shop shop : value) {
            //stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
        locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
        }
        stringRedisTemplate.opsForGeo().add(key,locations);
    }

}

}
