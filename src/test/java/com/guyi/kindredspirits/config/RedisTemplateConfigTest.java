package com.guyi.kindredspirits.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * 测试自定义 RedisTemplateConfig
 */
@SpringBootTest
public class RedisTemplateConfigTest {
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testRedisTemplate(){
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("zs", 10);
        valueOperations.set("ls", "20");
        // 查
        Object zs = valueOperations.get("zs");
        Assertions.assertTrue(10 == (Integer) zs);
        Object ls = valueOperations.get("ls");
        Assertions.assertTrue("20".equals(ls));
        // 删除
        redisTemplate.delete("zs");
        redisTemplate.delete("ls");
    }
}
