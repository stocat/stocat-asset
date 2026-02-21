package com.stocat.asset.redis;

import com.stocat.asset.redis.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {RedisConfig.class})
class StocatRedisApplicationTests {

    @Test
    void contextLoads() {
    }

}
