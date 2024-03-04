package com.guyi.kindredspirits.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.AlgorithmUtil;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private RedissonClient redissonClient;

    /**
     * 预热热点用户的普通推荐数据。<br/>
     * 每天 23:59:00 执行, 只有一台服务器会执行。
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void doCacheRecommendUser() {
        // 缓存预热
        final String lockKey = String.format(RedisConstant.KEY_PRE, "precache-job", "do-cache", "lock");
        // 确保只有一台服务器执行这个任务
        Boolean result = LockUtil.opsRedissonLock(lockKey,
                0,
                RedisConstant.SCHEDULED_LOCK_LEASE_TIME,
                TimeUnit.SECONDS,
                redissonClient,
                this::cacheRecommendUser);

        // 问题记录
        result = Optional.ofNullable(result).orElse(false);
        if (!result) {
            log.error("doCacheRecommendUser 缓存预热失败");
        }
    }

    /**
     * 缓存为热点用户的普通推荐数据
     */
    private boolean cacheRecommendUser() {
        // 查询所有热点用户数据
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "email", "phone", "profile")
                .eq("isHot", UserConstant.HOT_USER_TAG);
        List<User> mainUserList = userMapper.selectList(userQueryWrapper);

        // 寻找推荐用户
        for (User mainUser : mainUserList) {
            // 查询所有用户信息
            userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper
                    .select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "profile", "phone", "email")
                    .ne("id", mainUser.getId());
            List<User> userList = userService.list(userQueryWrapper);
            Map<String, List<Integer>> loginUserTagMap = userService.getTagWeightList(mainUser.getTags());

            List<User> cacheUserList = new ArrayList<>();
            for (User user : userList) {
                String userTags = user.getTags();
                // 排除未设置标签用户和自己
                if (StringUtils.isBlank(userTags) || mainUser.getId().equals(user.getId())) {
                    continue;
                }

                // 匹配推荐用户
                Map<String, List<Integer>> otherUserTagMap = userService.getTagWeightList(user.getTags());
                double similarity = AlgorithmUtil.similarity(loginUserTagMap, otherUserTagMap);
                if (similarity > 0.7) {
                    cacheUserList.add(user);
                    user.setTags(userService.getTagListJson(user));
                }
            }

            // 写缓存, key 超时时间 = 15 小时 + 随机时间(分钟)
            /*String redisKey = String.format(RedisConstant.RECOMMEND_KEY_PRE, mainUser.getId());
            long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(15 * 60L);
            boolean result = RedisUtil.setValue(redisKey, cacheUserList, timeout, TimeUnit.MINUTES);
            if (!result) {
                log.error("id 为 {} 的用户进行缓存预热时出现问题", mainUser.getId());
            }*/

            final String recommendKey = String.format(RedisConstant.RECOMMEND_KEY_PRE, mainUser.getId());
            int size = cacheUserList.size();
            int pageSize = 10;
            long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(5 * 60L);
            if (size <= pageSize) {
                String redisHashKey = "1";
                boolean result = RedisUtil.setHashValue(recommendKey,
                        redisHashKey,
                        cacheUserList,
                        timeout,
                        TimeUnit.MINUTES);
                if (!result) {
                    log.error("id 为 {} 的用户进行缓存预热时出现问题", mainUser.getId());
                }
                continue;
            }
            for (int pageNum = 1; pageNum * pageSize <= size; pageNum++) {
                boolean result = RedisUtil.setHashValue(recommendKey,
                        String.valueOf(pageNum),
                        cacheUserList,
                        timeout,
                        TimeUnit.MINUTES);
                if (!result) {
                    log.error("id 为 {} 的用户进行缓存预热时出现问题, 出错的页为 {}", mainUser.getId(), pageNum);
                }
            }
        }

        return true;
    }

}
