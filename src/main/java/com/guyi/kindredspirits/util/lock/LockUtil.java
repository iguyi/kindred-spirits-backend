package com.guyi.kindredspirits.util.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 简化锁操作的工具类
 *
 * @author 孤诣
 */
@Slf4j
public class LockUtil {

    private LockUtil() {
    }

    /**
     * Redisson 实现 Redis 分布式锁
     *
     * @param lockKey        - 锁对应的 key
     * @param leaseTime      - 持有锁的时间
     * @param timeUnit       - 时间单位
     * @param redissonClient - Redisson 客户端
     * @param callback       - 回调函数, 用于定义需要执行的任务
     */
    public static void opsRedissonLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit,
                                       RedissonClient redissonClient, LockCallback callback) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
                callback.execute();
            }
        } catch (InterruptedException e) {
            log.debug("{}#opsRedissonLock 发生异常, 异常信息如下: \n", LockUtil.class.getName(), e);
        } finally {
            // 只释放当前线程加的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
