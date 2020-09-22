package com.gulimall.product.product;







import com.atguigu.gulimall.product.GulimallProductApplication;
import com.atguigu.gulimall.product.app.CategoryController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = GulimallProductApplication.class)
public class GulimallProductApplicationTests {


    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    public void test1() {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set("hello","world");
        String hello = ops.get("hello");
        System.out.println(hello);
    }

}
