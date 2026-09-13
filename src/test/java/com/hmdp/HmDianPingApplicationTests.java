package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheCllent;
import com.hmdp.utils.ReidsWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {
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

}
