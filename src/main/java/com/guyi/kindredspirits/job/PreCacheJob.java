package com.guyi.kindredspirits.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.reflect.TypeToken;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.AlgorithmUtil;
import com.guyi.kindredspirits.util.JsonUtil;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.*;
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
        System.out.println("缓存完成");
    }

    /**
     * 缓存为热点用户的普通推荐数据
     */
    private boolean cacheRecommendUser() {
        // 查询所有热点用户数据
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // "id", "userAccount", "username", "avatarUrl", "gender", "tags", "profile", "phone", "email"
        userQueryWrapper.select("id", "username", "avatarUrl", "tags").eq("isHot", UserConstant.HOT_USER_TAG);
        List<User> mainUserList = userMapper.selectList(userQueryWrapper);

        // 查询所有用户信息
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "username", "avatarUrl", "tags");
        List<User> userList = userService.list(userQueryWrapper);

        // 寻找推荐用户
        for (User mainUser : mainUserList) {
            // 解析当前热点用户的标签数据
            String mainTags = mainUser.getTags();
            if (StringUtils.isBlank(mainTags)) {
                continue;
            }
            Map<String, List<Integer>> mainUserTagMap = userService.getTagWeightList(mainTags);

            // 深拷贝数据
            String userListDeepCopyJson = JsonUtil.G.toJson(userList);
            Type userListType = new TypeToken<List<User>>() {
            }.getType();
            List<User> userListDeepCopy = JsonUtil.fromJson(userListDeepCopyJson, userListType);

            // 选择缓存的数据
            Long mainUserId = mainUser.getId();
            List<User> cacheUserList = new ArrayList<>();
            for (User user : userListDeepCopy) {
                // 排除未设置标签用户和自己
                String userTags = user.getTags();
                if (StringUtils.isBlank(userTags) || Objects.equals(mainUserId, user.getId())) {
                    continue;
                }

                Map<String, List<Integer>> otherUserTagMap = userService.getTagWeightList(userTags);
                double similarity = AlgorithmUtil.similarityPro(mainUserTagMap, otherUserTagMap);
                if (similarity > 0.4) {
                    cacheUserList.add(user);
                    user.setTags(userService.getTagListJson(user));
                }

                if (cacheUserList.size() > 200) {
                    break;
                }
            }

            // 分页缓存数据
            final String recommendKey = String.format(RedisConstant.RECOMMEND_KEY_PRE, mainUserId);
            int size = cacheUserList.size();
            int pageSize = 10;
            long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(5 * 60L);
            // 总数据量不满一页, 按一页计算
            if (size != 0 && size <= pageSize) {
                String redisHashKey = "1";
                saveCacheByPage(mainUserId, cacheUserList, recommendKey, timeout, redisHashKey);
                continue;
            }
            // 总数据量大于一页的数据量
            for (int pageNum = 1; pageNum * pageSize <= size; pageNum++) {
                int starIndex = (pageNum - 1) * pageSize;
                List<User> userListChild = cacheUserList.subList(starIndex, starIndex + pageSize);
                saveCacheByPage(mainUserId, userListChild, recommendKey, timeout, String.valueOf(pageNum));
            }
        }

        return true;
    }

    /**
     * 按页缓存用户普通推荐数据
     *
     * @param mainUserId - 热点用户Id
     * @param pageData   - 当前页对应的数据
     * @param redisKey   - redis key
     * @param timeout    - 超时时间, 单位: min
     * @param pageNum    - 页码
     */
    private void saveCacheByPage(Long mainUserId, List<User> pageData, String redisKey, long timeout, String pageNum) {
        boolean result = RedisUtil.setHashValue(redisKey, pageNum, pageData, timeout, TimeUnit.MINUTES);
        if (!result) {
            log.error("id 为 {} 的用户进行缓存预热时出现问题, 出错的页为 {}", mainUserId, pageNum);
        }
    }

}
