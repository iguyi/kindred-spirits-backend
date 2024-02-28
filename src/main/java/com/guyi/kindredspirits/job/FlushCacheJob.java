package com.guyi.kindredspirits.job;

import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 缓存刷新
 *
 * @author 孤诣
 */
@Component
@Slf4j
public class FlushCacheJob {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 清理过期的会话未读消息记录的定时任务。<br/>
     * 每 5 分钟执行一次, 且只会被一台服务器执行。
     */
    @Scheduled(cron = "0 0/5 * * * *")
    public void doClearCacheUnreadMessageNum() {
        final String lockKey = String.format(RedisConstant.KEY_PRE, "precache-job", "do-cache", "lock");
        LockUtil.opsRedissonLock(lockKey, 0, RedisConstant.SCHEDULED_LOCK_LEASE_TIME, TimeUnit.SECONDS,
                redissonClient, this::clearCacheUnreadMessageNum);
    }

    /**
     * 清理过期的会话未读消息记录
     */
    public void clearCacheUnreadMessageNum() {
        // 当前时间
        long currentTimeMillis = System.currentTimeMillis();

        // 获取缓存中所有会话的名称
        final String sessionStateKeyListKey = RedisConstant.SESSION_STATE_KEY_LIST;
        List<String> sessionStateKeyList = RedisUtil.STRING_REDIS_TEMPLATE
                .opsForList()
                .range(sessionStateKeyListKey, 0, -1);
        if (CollectionUtils.isEmpty(sessionStateKeyList)) {
            return;
        }

        // 记录被清理的会话
        List<String> clearSessionStateKeyList = new ArrayList<>();

        // 清理过期的会话缓存
        sessionStateKeyList.forEach(sessionStateKey -> {
            if (!RedisUtil.hasRedisKey(sessionStateKey)) {
                return;
            }
            Map<Object, Object> map = RedisUtil.STRING_REDIS_TEMPLATE.opsForHash().entries(sessionStateKey);
            long expirationTime = Long.parseLong(map.get("expiration_time").toString());
            if (expirationTime - currentTimeMillis <= 0) {
                Boolean result = RedisUtil.STRING_REDIS_TEMPLATE.delete(sessionStateKey);
                result = Optional.ofNullable(result).orElse(false);
                if (result) {
                    clearSessionStateKeyList.add(sessionStateKey);
                }
            }
        });

        // 更新缓存中存在 session 的情况
        if (clearSessionStateKeyList.isEmpty()) {
            return;
        }
        sessionStateKeyList.removeAll(clearSessionStateKeyList);
        boolean result = RedisUtil.setListValue(sessionStateKeyListKey, sessionStateKeyList, null, null);
        if (!result) {
            log.error("FlushCacheJob.clearCacheUnreadMessageNum 对 {} 的缓存失败了", sessionStateKeyList);
        }
    }

}
