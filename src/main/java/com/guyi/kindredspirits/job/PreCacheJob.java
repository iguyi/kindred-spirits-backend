package com.guyi.kindredspirits.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guyi.kindredspirits.controller.UserController;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 */
@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 重点用户的 ID
     */
    private List<Long> mainUserList = Arrays.asList(1L);

    private static final String lockKeyPre = "kindredspirits:precachejob:%s:%s";

    /**
     * 预热推荐用户。
     * 每天 23:59:00 执行。
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void doCacheRecommendUser() {
        final String lockKey = String.format(lockKeyPre, "docache", "lock");  // 获取锁对象
        RLock lock = redissonClient.getLock(lockKey);    // 获取锁对象
        try {
            if (lock.tryLock(0, 30L, TimeUnit.SECONDS)) { // 尝试获取锁, 没获取到锁就不执行(等待)了
                for (Long mainUser : mainUserList) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                    String redisKey = String.format(UserController.redisKeyPre, "recommend", mainUser);
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    // 写缓存
                    try {
                        // todo 缓存击穿、缓存雪崩的问题
                        valueOperations.set(redisKey, userPage, 120, TimeUnit.MINUTES);  // 120 分钟
                    } catch (Exception e) {
                        log.error("redis set key error: ", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.debug("The doCacheRecommendUser method of the PreCacheJob class is error: " + e);
        } finally {
            if (lock.isHeldByCurrentThread()) {  // 只释放当前线程加的锁
                lock.unlock();
            }
        }
    }
}
