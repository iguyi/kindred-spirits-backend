package com.guyi.kindredspirits.util.redis;

import lombok.Data;

/**
 * Redis 缓存查询返回结果
 *
 * @author 孤诣
 */
@Data
public class RedisQueryReturn<D> {

    /**
     * 数据本身
     */
    private D data;

    /**
     * 数据缓存是否过期
     * - true: 已过期
     * - false: 未过期
     */
    private boolean expiration;

    /**
     * 数据对应缓存是否设置有 expiration_time 字段
     * - true: 有设置
     * - false: 未设置
     */
    private boolean existExpirationTime;

}
