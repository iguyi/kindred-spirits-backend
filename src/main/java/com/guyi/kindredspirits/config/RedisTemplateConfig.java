package com.guyi.kindredspirits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 模板配置
 */
@Configuration
public class RedisTemplateConfig {
    /**
     * 自定义序列化器
     *
     * @param connectionFactory - Redis 连接工厂
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);  // 设置连接工厂
        // 对于数据的 key, 使用 StringRedisSerializer 序列化器,
        redisTemplate.setKeySerializer(RedisSerializer.string());
        return redisTemplate;
    }
}
