package com.guyi.kindredspirits.common.contant;

/**
 * Redis 常量
 *
 * @author 孤诣
 */
public interface RedisConstant {

    /**
     * Redis 数据的 key 模板字符串
     */
    String KEY_PRE = "kindred-spirits:%s:%s:%s";


    /**
     * Redis 推荐用户的缓存数据 key 模板
     */
    String RECOMMEND_KEY_PRE = "kindred-spirits:user:recommend:%s";

    /**
     * 锁的 key 模板
     */
    String LOCK_KEY = "kindred-spirits:lock:%s:%s:%s";

    /**
     * Redis 验证消息缓存的 key 模板
     */
    String MESSAGE_VERIFY_KEY_PRE = "kindred-spirits:message:verify:%s";

    /**
     * 缓存 "最大 ID 用户的账号" 的 key
     */
    String MAX_ID_USER_ACCOUNT_KEY = "kindred-spirits:user:max:id";

    /**
     * 对应存放 "会话状态缓存 key" 的列表的 key
     */
    String SESSION_STATE_KEY_LIST = String.format(RedisConstant.KEY_PRE, "unread", "session-state-key", "list");

    /**
     * 缓存预热存储的数据的过期时间 - 15 个小时
     */
    Long PRECACHE_TIMEOUT = 900L;

    /**
     * 定时任务锁的失效时间
     */
    Long SCHEDULED_LOCK_LEASE_TIME = 30L;

}
