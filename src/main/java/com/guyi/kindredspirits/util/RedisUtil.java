package com.guyi.kindredspirits.util;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * @author 张仕恒
 */
public class RedisUtil {

    /**
     * 写缓存 - 字符串类型数据
     */
    public static <D> void setForValue(RedisTemplate<String, String> redisTemplate,
                                       String key, D data, Long timeout, TimeUnit unit) {
        String jsonObj = JsonUtil.G.toJson(data);
        redisTemplate.opsForValue().set(key, jsonObj, timeout, unit);
    }

    /**
     * 写缓存 - 字符串类型数据
     * 不设置过期时间
     */
    public static <D> void setForValue(RedisTemplate<String, String> redisTemplate, String key, D data) {
        String jsonObj = JsonUtil.G.toJson(data);
        redisTemplate.opsForValue().set(key, jsonObj);
    }

    /**
     * 读缓存 - 字符串类型数据
     */
    public static <R> R getForValue(RedisTemplate<String, String> redisTemplate, String key, Class<R> type) {
        String jsonObj = redisTemplate.opsForValue().get(key);
        return JsonUtil.fromJson(jsonObj, type);
    }

}
