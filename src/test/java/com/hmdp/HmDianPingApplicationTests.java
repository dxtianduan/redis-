package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheCllent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {
@Resource
    private ShopServiceImpl shopService;
    @Resource
    private CacheCllent cacheCllent;
@Test
   public void testSaveShop(){

}

}
