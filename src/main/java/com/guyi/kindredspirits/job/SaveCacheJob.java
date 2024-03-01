package com.guyi.kindredspirits.job;

import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 将 Redis 缓存中的数据存入 MySQL 中
 *
 * @author 孤诣
 */
@Component
@Slf4j
public class SaveCacheJob {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UnreadMessageNumService unreadMessageNumService;

    /**
     * 最近一次保存到 MySQL 中的会话状态. <br/>
     * 外层 Map: <br/>
     * - key: sessionName <br/>
     * - value: 具体数据<br/><br/>
     * 内层 Map: <br/>
     * - key: hashKey <br/>
     * - value: hashValue
     */
    private static final Map<String, Map<Object, Object>> OLD_SESSION_STATE = new HashMap<>();

    /**
     * 将 Redis 缓存中的未读消息数消息写入 MySQL 的定时任务<br/>
     * 每 5 分钟执行一次
     */
    @Scheduled(cron = "0 0/5 * * * *")
    public void saveSessionSateJob() {
        String lockKey = String.format(RedisConstant.LOCK_KEY, "save", "state", "unread");
        LockUtil.opsRedissonLock(lockKey,
                0,
                RedisConstant.SCHEDULED_LOCK_LEASE_TIME,
                TimeUnit.SECONDS,
                redissonClient,
                saveSessionState());
    }

    /**
     * 将 Redis 缓存中的未读消息数消息写入 MySQL
     *
     * @return 可运行对象
     */
    public Runnable saveSessionState() {
        return () -> {
            // 判断 key 是否存在
            String sessionStateKeyListKey = RedisConstant.SESSION_STATE_KEY_LIST;
            boolean hasRedisKey = RedisUtil.hasRedisKey(sessionStateKeyListKey);
            if (!hasRedisKey) {
                return;
            }

            // 获取所有未读(聊天)消息数据
            StringRedisTemplate stringRedisTemplate = RedisUtil.STRING_REDIS_TEMPLATE;
            List<String> sessionNameList = stringRedisTemplate.opsForList().range(sessionStateKeyListKey, 0, -1);

            // 没有相关数据
            if (CollectionUtils.isEmpty(sessionNameList)) {
                return;
            }

            // 对数据进行处理: 处理为正确格式、去重
            Set<String> sessionNameSet = sessionNameList.stream()
                    .map(sessionName -> sessionName.replace("\"", ""))
                    .collect(Collectors.toSet());

            // 收集需要保存的数据
            List<UnreadMessageNum> unreadMessageNumList = new ArrayList<>();
            for (String sessionName : sessionNameSet) {
                // 将数据存入 MySQL
                if (RedisUtil.hasRedisKey(sessionName)) {
                    // 从缓存中获取对应数据
                    Map<Object, Object> dataMap = stringRedisTemplate.opsForHash().entries(sessionName);

                    // 避免不必要的数据更新(在最近更新过的数据)
                    if (OLD_SESSION_STATE.containsKey(sessionName)) {
                        if (Objects.equals(OLD_SESSION_STATE.get(sessionName), dataMap)) {
                            continue;
                        }
                    }

                    // 组合需要更新到 MySQL 的数据
                    UnreadMessageNum unreadMessageNum = new UnreadMessageNum();
                    unreadMessageNum.setId(Long.parseLong((String) dataMap.get("id")));
                    unreadMessageNum.setUnreadNum(Integer.valueOf((String) dataMap.get("unreadNum")));
                    unreadMessageNumList.add(unreadMessageNum);

                    // 记录
                    OLD_SESSION_STATE.put(sessionName, dataMap);
                }
            }

            // 没有需要保存的数据
            if (unreadMessageNumList.isEmpty()) {
                return;
            }

            // 批量保存数据
            unreadMessageNumService.saveOrUpdateBatch(unreadMessageNumList, unreadMessageNumList.size());

            // 更新 session name 的 key 列表
            Boolean result = stringRedisTemplate.delete(sessionStateKeyListKey);
            result = Optional.ofNullable(result).orElse(false);
            if (result) {
                RedisUtil.setListValue(sessionStateKeyListKey,
                        new ArrayList<>(sessionNameSet),
                        null,
                        null);
            }
        };
    }

}
