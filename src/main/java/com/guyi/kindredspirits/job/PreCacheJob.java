package com.guyi.kindredspirits.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guyi.kindredspirits.contant.RedisConstant;
import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.AlgorithmUtil;
import com.guyi.kindredspirits.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 *
 * @author 孤诣
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 预热热点用户。
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
                // 查询所有热点用户数据
                QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper.select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "email",
                                "phone", "profile")
                        .eq("isHot", UserConstant.HOT_USER_TAG);
                List<User> mainUserList = userMapper.selectList(userQueryWrapper);

                for (User mainUser : mainUserList) {
                    // 查询所有用户信息
                    userQueryWrapper = new QueryWrapper<>();
                    userQueryWrapper.select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "profile"
                            , "phone", "email");
                    userQueryWrapper.ne("id", mainUser.getId());
                    userQueryWrapper.isNotNull("tags");
                    List<User> userList = userService.list(userQueryWrapper);

                    Map<String, List<Integer>> loginUserTagMap = userService.getTagWeightList(mainUser.getTags());
                    List<User> cacheUserList = new ArrayList<>();
                    for (User user : userList) {
                        String userTags = user.getTags();
                        // 排除未设置标签用户和自己
                        if (StringUtils.isBlank(userTags) || mainUser.getId().equals(user.getId())) {
                            continue;
                        }
                        Map<String, List<Integer>> otherUserTagMap = userService.getTagWeightList(user.getTags());
                        double similarity = AlgorithmUtil.similarity(loginUserTagMap, otherUserTagMap);
                        // 相似度大于 0.7 才认为二者相似
                        if (similarity > 0.7) {
                            cacheUserList.add(user);
                            user.setTags(userService.getTagListJson(user));
                        }
                    }

                    String redisKey = String.format(RedisConstant.RECOMMEND_KEY_PRE, mainUser.getId());
                    try {
                        // 写缓存, key 超时时间 = 15 小时 + 随机时间(分钟)
                        // todo 缓存问题
                        long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(15 * 60L);
                        boolean result = RedisUtil.setForValue(redisKey, cacheUserList, timeout,
                                TimeUnit.MINUTES);
                        if (!result) {
                            log.error("redis set {} error.", redisKey);
                        }
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
