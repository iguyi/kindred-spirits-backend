package com.guyi.kindredspirits.contant;

/**
 * Redis 常量
 *
 * @author 张仕恒
 */
public interface RedisConstant {

    /**
     * Redis 数据的 key 模板字符串
     */
    String KEY_PRE = "kindred-spirits:%s:%s:%s";

    /**
     * 缓存预热存储的数据的过期时间 - 15 个小时
     */
    Long PRECACHE_TIMEOUT = 900L;

    /**
     * 定时任务锁的失效时间
     */
    Long SCHEDULED_LOCK_LEASE_TIME = 30L;

}
