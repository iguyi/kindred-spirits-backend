package com.guyi.kindredspirits.util.redis;

import cn.hutool.extra.spring.SpringUtil;
import com.guyi.kindredspirits.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * @author 孤诣
 */
@Slf4j
public class RedisUtil {

    /**
     * Redis 操作模板
     */
    private static final StringRedisTemplate STRING_REDIS_TEMPLATE;

    /**
     * 记录数据的 hash key
     */
    private static final String DATA_HASH_KEY = "data";

    /**
     * 记录数据过期时间的 hash key
     */
    private static final String TIMEOUT_HASH_KEY = "expiration_time";

    static {
        STRING_REDIS_TEMPLATE = SpringUtil.getBean(StringRedisTemplate.class);
    }

    /**
     * 将 data 以 JSON 字符串的形式写入 Redis 缓存
     * 说明: 数据的过期时间是逻辑过期
     *
     * @param key     - 数据对应的缓存 key
     * @param data    - 数据
     * @param timeout - 过期时间, 逻辑过期
     * @param unit    - 过期时间的单位
     * @param <D>     - 源数据对应的泛型
     * @return true: 缓存成功; false: 缓存失败
     */
    public static <D> boolean setValue(String key, D data, Long timeout, TimeUnit unit) {
        return setHashValue(key, DATA_HASH_KEY, data, timeout, unit);
    }

    /**
     * 获取缓存数据
     *
     * @param key  - 数据对应的缓存 key
     * @param typeOfD - 期望得到数据的泛型类型信息
     * @param <D>  期望的返回值类型
     * @return 查询结果封装对象
     */
    public static <D> RedisQueryReturn<D> getValue(String key, Type typeOfD) {
        return getHashValue(key, DATA_HASH_KEY, typeOfD);
    }

    /**
     * 将 data 转为 JSON 格式字符串后, 以 hash 形式进行缓存。
     * 说明: 数据的过期时间是逻辑过期
     *
     * @param key     - 数据对应的缓存 key
     * @param hashKey - 数据对应缓存内的 hash key
     * @param data    - 数据
     * @param timeout - 逻辑过期时间
     * @param unit    - 过期时间的单位
     * @param <D>     - 源数据对应的泛型
     * @return true: 缓存成功; false: 缓存失败
     */
    public static <D> boolean setHashValue(String key, String hashKey, D data, Long timeout, TimeUnit unit) {
        try {
            long timeoutMillis = System.currentTimeMillis() + unit.toMillis(timeout);
            String jsonObj = JsonUtil.G.toJson(data);
            HashOperations<String, Object, Object> opsForHash = STRING_REDIS_TEMPLATE.opsForHash();
            opsForHash.put(key, hashKey, jsonObj);
            opsForHash.put(key, TIMEOUT_HASH_KEY, String.valueOf(timeoutMillis));
            return true;
        } catch (Exception e) {
            log.error("{}#setHashValue() 设置 Redis 缓存出错, 错误信息如下: \n", RedisUtil.class.getName(), e);
            return false;
        }
    }

    /**
     * 获取 hash 结构中的数据
     *
     * @param key     - 数据对应的缓存 key
     * @param hashKey - 数据对应缓存内的 hash key
     * @param typeOfD    - 期望得到数据的泛型类型信息
     * @param <D>     - 期望的返回值类型
     * @return 查询结果封装对象
     */
    public static <D> RedisQueryReturn<D> getHashValue(String key, String hashKey, Type typeOfD) {
        try {
            HashOperations<String, Object, Object> opsForHash = STRING_REDIS_TEMPLATE.opsForHash();

            // 获取目标数据
            String jsonObj = (String) opsForHash.get(key, hashKey);
            D data = JsonUtil.fromJson(jsonObj, typeOfD);
            RedisQueryReturn<D> result = new RedisQueryReturn<>();
            result.setData(data);

            // 判断是否有设置 expiration_time 字段
            Boolean existExpirationTime = opsForHash.hasKey(key, TIMEOUT_HASH_KEY);
            if (existExpirationTime) {
                // 有设置 expiration_time 字段,
                result.setExistExpirationTime(true);

                // 根据 expiration_time 字段的信息, 判断数据是否过期
                String expirationTimeStr = (String) opsForHash.get(key, TIMEOUT_HASH_KEY);
                expirationTimeStr = Optional.ofNullable(expirationTimeStr).orElse("0L");
                long expirationTimeNum = Long.parseLong(expirationTimeStr);
                result.setExpiration(expirationTimeNum <= System.currentTimeMillis());
            } else {
                // 未设置 expiration_time 字段
                result.setExpiration(false);
                result.setExistExpirationTime(false);
            }
            return result;
        } catch (Exception e) {
            log.error("{}#setHashValue() 获取 Redis 缓存出错, 错误信息如下: \n", RedisUtil.class.getName(), e);
            return null;
        }
    }

    /**
     * 判断 Redis Key 是否存在
     *
     * @param redisKey - Redis Key
     * @return Redis Key 存在时返回 true; 反之, 返回 false
     */
    public static boolean hasRedisKey(String redisKey) {
        Boolean hasKey = STRING_REDIS_TEMPLATE.hasKey(redisKey);
        return Optional.ofNullable(hasKey).orElse(false);
    }

}
