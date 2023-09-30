package com.guyi.kindredspirits.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    /**
     * Redis 服务器主机, 从配置中获取
     */
    private String host;

    /**
     * Redis 端口, 从配置中获取
     */
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 创建配置对象
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(1);

        // 创建实例
        return Redisson.create(config);
    }
}
