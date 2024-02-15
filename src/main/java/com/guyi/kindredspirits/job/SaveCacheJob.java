package com.guyi.kindredspirits.job;

import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.redisson.api.RedissonClient;
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
            boolean hasRedisKey = RedisUtil.hasRedisKey(RedisConstant.SESSION_STATE_KEY_LIST);
            if (!hasRedisKey) {
                return;
            }

            // 获取所有未读(聊天)消息数据
            List<String> sessionNameList = RedisUtil.STRING_REDIS_TEMPLATE.opsForList()
                    .range(RedisConstant.SESSION_STATE_KEY_LIST, 0, -1);
            if (CollectionUtils.isEmpty(sessionNameList)) {
                return;
            }
            // 去重
            Set<String> sessionNameSet = sessionNameList.stream()
                    .map(sessionName -> sessionName.replace("\"", ""))
                    .collect(Collectors.toSet());
            List<UnreadMessageNum> unreadMessageNumList = new ArrayList<>();
            for (String sessionName : sessionNameSet) {
                if (RedisUtil.hasRedisKey(sessionName)) {
                    // 将数据存入 MySQL
                    Map<Object, Object> dataMap = RedisUtil.STRING_REDIS_TEMPLATE.opsForHash().entries(sessionName);
                    // 避免不必要的数据更新
                    if (OLD_SESSION_STATE.containsKey(sessionName)) {
                        if (Objects.equals(OLD_SESSION_STATE.get(sessionName), dataMap)) {
                            continue;
                        }
                    }
                    UnreadMessageNum unreadMessageNum = new UnreadMessageNum();
                    unreadMessageNum.setId(Long.parseLong((String) dataMap.get("id")));
                    unreadMessageNum.setUnreadNum(Integer.valueOf((String) dataMap.get("unreadNum")));
                    unreadMessageNumList.add(unreadMessageNum);
                    OLD_SESSION_STATE.put(sessionName, dataMap);
                }
            }
            // 批量保存数据
            unreadMessageNumService.saveOrUpdateBatch(unreadMessageNumList, unreadMessageNumList.size());

            // 更新 session name 的 key 列表
            Boolean result = RedisUtil.STRING_REDIS_TEMPLATE.delete(RedisConstant.SESSION_STATE_KEY_LIST);
            result = Optional.ofNullable(result).orElse(false);
            if (result) {
                RedisUtil.setListValue(RedisConstant.SESSION_STATE_KEY_LIST,
                        new ArrayList<>(sessionNameSet),
                        null,
                        null);
            }
        };
    }

}
