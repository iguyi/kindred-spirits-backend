package com.guyi.kindredspirits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 模板配置
 *
 * @author 张仕恒
 */
@Configuration
public class RedisTemplateConfig {
    /**
     * 自定义序列化器
     *
     * @param connectionFactory - Redis 连接工厂, Spring 自动注入
     * @return redisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);

        RedisSerializer<String> redisStringSerializer = RedisSerializer.string();

        // 设置 key 和 hashKey 的序列化器
        redisTemplate.setKeySerializer(redisStringSerializer);
        redisTemplate.setHashKeySerializer(redisStringSerializer);

        // 设置 value 和 hashValue 的序列化器
        redisTemplate.setValueSerializer(redisStringSerializer);
        redisTemplate.setHashValueSerializer(redisStringSerializer);

        return redisTemplate;
    }
}
