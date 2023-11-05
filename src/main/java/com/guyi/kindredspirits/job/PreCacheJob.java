package com.guyi.kindredspirits.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guyi.kindredspirits.contant.RedisConstant;
import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 *
 * @author 张仕恒
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 预热推荐用户。
     * 每天 23:59:00 执行。
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void doCacheRecommendUser() {
        // 获取锁对象
        final String lockKey = String.format(RedisConstant.KEY_PRE, "precache-job", "do-cache", "lock");

        // 获取锁对象
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁, 没获取到锁就不执行(等待)了
            if (lock.tryLock(0, RedisConstant.SCHEDULED_LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper.select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "email",
                        "phone", "profile").eq("isHot", UserConstant.HOT_USER_TAG);
                List<User> mainUserList = userMapper.selectList(userQueryWrapper);
                // todo 遍历 MainUserList, 匹配每个热点用户对应的相似用户, 存储 Page<User>
                for (User mainUser : mainUserList) {
                    String redisKey = String.format(RedisConstant.KEY_PRE, "user", "recommend", mainUser.getId());
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    // 写缓存
                    try {
                        // todo 缓存问题
                        // 15 小时 + 随机时间
                        long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(15 * 60L);
                        valueOperations.set(redisKey, mainUser, timeout, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("redis set key error: ", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.debug("The doCacheRecommendUser method of the PreCacheJob class is error: " + e);
        } finally {
            // 只释放当前线程加的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
