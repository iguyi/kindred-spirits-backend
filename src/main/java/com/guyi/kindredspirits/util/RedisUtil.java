package com.guyi.kindredspirits.util;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * todo 待完善
 *
 * @author 孤诣
 */
@Slf4j
public class RedisUtil {

    private static final StringRedisTemplate STRING_REDIS_TEMPLATE;

    private static final String DATA_HASH_KEY = "data";

    private static final String TIMEOUT_HASH_KEY = "time";

    static {
        STRING_REDIS_TEMPLATE = SpringUtil.getBean(StringRedisTemplate.class);
    }

    /**
     * 写缓存 - 字符串类型数据
     */
    public static <D> boolean setForValue(String key, D data, Long timeout, TimeUnit unit) {
        try {
            String jsonObj = JsonUtil.G.toJson(data);
            timeout = System.currentTimeMillis() + unit.toMillis(timeout);
            STRING_REDIS_TEMPLATE.opsForHash().put(key, DATA_HASH_KEY, jsonObj);
            STRING_REDIS_TEMPLATE.opsForHash().put(key, TIMEOUT_HASH_KEY, String.valueOf(timeout));
            return true;
        } catch (Exception e) {
            log.error("Set cache error in method setForValue of class RedisUtil, error message: \n", e);
            return false;
        }
    }

    /**
     * 写缓存 - 字符串类型数据
     * 不设置过期时间
     */
    public static <D> boolean setForValue(String key, D data) {
        try {
            String jsonObj = JsonUtil.G.toJson(data);
            STRING_REDIS_TEMPLATE.opsForHash().put(key, DATA_HASH_KEY, jsonObj);
            return true;
        } catch (Exception e) {
            log.error("Set cache error in method setForValue of class RedisUtil, error message: \n", e);
            return false;
        }
    }

    /**
     * 获取缓存数据
     *
     * @param <R> - 期望的返回值类型
     */
    public static <R> R get(String key, Class<R> type) {
        try {
            String jsonObj = (String) STRING_REDIS_TEMPLATE.opsForHash().get(key, DATA_HASH_KEY);
            return JsonUtil.fromJson(jsonObj, type);
        } catch (Exception e) {
            log.error("Get cache error in method get of class RedisUtil, error message: \n", e);
            return null;
        }
    }

    /**
     * 判断数据是否过期
     *
     * @param key - redis key
     * @return <p> -1 表示未设置过期时间</p>
     * <P> 0 表示未过期</P>
     * <P> 1 表示已过期</P>
     * <P> 4 表示发生异常</P>
     */
    public static int expiration(String key) {
        try {
            String strTime = (String) STRING_REDIS_TEMPLATE.opsForHash().get(key, TIMEOUT_HASH_KEY);
            Long timeout = JsonUtil.fromJson(strTime, Long.class);
            if (timeout == null) {
                return -1;
            }
            return (timeout - System.currentTimeMillis() > 0) ? 0 : 1;
        } catch (Exception e) {
            log.error("Get cache error in method get of class RedisUtil, error message: \n", e);
            return 4;
        }
    }

}
