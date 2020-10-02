package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {
    /**
     * 所有对redis的使用都是通过RedissonClient对象
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        //建立配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.88.88:6379");
        //按照配置创建实例RedissonClient
        return Redisson.create(config);

    }

}
