package com.guyi.kindredspirits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(connectionFactory);
        stringRedisTemplate.setKeySerializer(RedisSerializer.string());
        stringRedisTemplate.setHashKeySerializer(RedisSerializer.string());
        stringRedisTemplate.setValueSerializer(RedisSerializer.string());
        stringRedisTemplate.setHashValueSerializer(RedisSerializer.string());
        return stringRedisTemplate;
    }

}
