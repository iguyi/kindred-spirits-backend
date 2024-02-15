package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.mapper.UnreadMessageNumMapper;
import com.guyi.kindredspirits.model.cache.UnreadMessageNumCache;
import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatSessionStateRequest;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 针对表 unread_message_num(未读聊天记录统计表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class UnreadMessageNumServiceImpl extends ServiceImpl<UnreadMessageNumMapper, UnreadMessageNum>
        implements UnreadMessageNumService {

    /**
     * 聊天会话状态的过期时间
     */
    private static final Long SESSION_STATE_EXPIRATION = 11L;

    /**
     * 聊天会话状态的过期时间的单位
     */
    private static final TimeUnit SESSION_STATE_EXPIRATION_UNIT = TimeUnit.MINUTES;

    @Override
    public void setSessionSate(User loginUser, ChatSessionStateRequest stateRequest) {
        // 参数校验
        if (stateRequest == null) {
            return;
        }
        Long id = stateRequest.getId();
        Boolean state = stateRequest.getState();
        Integer chatType = stateRequest.getChatType();
        ChatTypeEnum chantTypeEnum = ChatTypeEnum.getEnumByType(chatType);
        if (id == null || id < 1L || chantTypeEnum == null) {
            log.error("stateRequest field exist illegal value");
            return;
        }

        // 获取开启会话的用户和会话名称
        Long loginUserId = loginUser.getId();
        String chatSessionName = String.format("%s-%s-%s",
                ChatTypeEnum.PRIVATE_CHAT.equals(chantTypeEnum) ? "private" : "team",
                loginUserId,
                id);
        String redisKey = String.format(RedisConstant.SESSION_STATE_KEY, chatSessionName);

        Boolean safeState = Optional.ofNullable(state).orElse(false);
        if (RedisUtil.hasRedisKey(redisKey)) {
            // 对应会话的数据在缓存中存在, 变更状态
            RedisUtil.setHashValue(redisKey,
                    "isOpen",
                    safeState,
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT);
        } else {
            // 数据库查询会话的数据
            QueryWrapper<UnreadMessageNum> unreadMessageNumQueryWrapper = new QueryWrapper<>();
            unreadMessageNumQueryWrapper.select("id", "unreadNum")
                    .eq("userId", loginUserId)
                    .eq("chatSessionName", chatSessionName);
            UnreadMessageNum target = this.getOne(unreadMessageNumQueryWrapper);
            if (target == null) {
                // 对应会话数据不存在
                return;
            }

            // 设置 id
            RedisUtil.setHashValue(redisKey,
                    "id",
                    target.getId(),
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT
            );

            // 设置未读消息数
            RedisUtil.setHashValue(redisKey,
                    "unreadNum",
                    Optional.ofNullable(target.getUnreadNum()).orElse(0),
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT
            );

            // 设置当前会话是否打开(用户是否正处于对应聊天窗口内)
            RedisUtil.setHashValue(redisKey,
                    "isOpen",
                    safeState,
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT
            );

            List<String> sessionStateKeyList = new ArrayList<>();
            sessionStateKeyList.add(redisKey);

            // 保存 Key
            boolean result =
                    RedisUtil.setListValue(RedisConstant.SESSION_STATE_KEY_LIST, sessionStateKeyList, null, null);
            if (!result) {
                log.error(sessionStateKeyList + "缓存失败");
            }
        }

        if (safeState) {
            // 未读消息数设置为 0
            RedisUtil.setHashValue(redisKey,
                    "unreadNum",
                    0,
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT
            );
        }
    }

    @Override
    public boolean updateUnreadMessageNum(String sessionName, int unreadNum) {
        String redisKey = String.format(RedisConstant.SESSION_STATE_KEY, sessionName);
        if (RedisUtil.hasRedisKey(redisKey)) {
            // 对应会话的数据在缓存中存在, 直接更新未读消息数
            return RedisUtil.setHashValue(redisKey,
                    "unreadNum",
                    unreadNum,
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT
            );
        }

        // 数据库查询会话的数据
        QueryWrapper<UnreadMessageNum> unreadMessageNumQueryWrapper = new QueryWrapper<>();
        unreadMessageNumQueryWrapper.select("id", "unreadNum").eq("chatSessionName", sessionName);
        UnreadMessageNum target = this.getOne(unreadMessageNumQueryWrapper);
        if (target == null) {
            // 对应会话数据不存在
            return false;
        }

        // 设置 id
        boolean cacheId = RedisUtil.setHashValue(redisKey,
                "id",
                target.getId(),
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 设置未读消息数
        boolean cacheUnreadNum = RedisUtil.setHashValue(redisKey,
                "unreadNum",
                unreadNum,
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 设置当前会话是否打开(用户是否正处于对应聊天窗口内)
        boolean cacheIsOpen = RedisUtil.setHashValue(redisKey,
                "isOpen",
                false,
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        List<String> sessionStateKeyList = new ArrayList<>();
        sessionStateKeyList.add(redisKey);

        // 保存 Key
        boolean result =
                RedisUtil.setListValue(RedisConstant.SESSION_STATE_KEY_LIST, sessionStateKeyList, null, null);
        if (!result) {
            log.error(sessionStateKeyList + "缓存失败");
        }
        return cacheId && cacheUnreadNum && cacheIsOpen && result;
    }

    /**
     * todo 缓存过期问题
     */
    @Override
    public UnreadMessageNumCache getUnreadMessageNumByName(String sessionName) {
        // 拼接 Redis Key
        String redisKey = String.format(RedisConstant.SESSION_STATE_KEY, sessionName);

        if (RedisUtil.hasRedisKey(redisKey)) {
            // 对应会话的数据在缓存中存在
            Map<Object, Object> map = RedisUtil.STRING_REDIS_TEMPLATE.opsForHash().entries(redisKey);
            UnreadMessageNumCache unreadMessageNumCache = new UnreadMessageNumCache();
            unreadMessageNumCache.setIsOpen(Boolean.valueOf(String.valueOf(map.get("isOpen"))));
            unreadMessageNumCache.setUnreadNum(Integer.valueOf(String.valueOf(map.get("unreadNum"))));
            unreadMessageNumCache.setId(Long.valueOf((String) map.get("id")));
            return unreadMessageNumCache;
        }

        // 对应会话的数据不在缓存中, 需要从 MySQL 中查询
        QueryWrapper<UnreadMessageNum> unreadMessageNumQueryWrapper = new QueryWrapper<>();
        unreadMessageNumQueryWrapper.select("id", "unreadNum").eq("chatSessionName", sessionName);
        UnreadMessageNum unreadMessageNum = this.getOne(unreadMessageNumQueryWrapper);
        if (unreadMessageNum == null) {
            return null;
        }
        UnreadMessageNumCache unreadMessageNumCache = new UnreadMessageNumCache();
        BeanUtils.copyProperties(unreadMessageNum, unreadMessageNumCache);
        unreadMessageNumCache.setIsOpen(false);

        // 设置 id
        RedisUtil.setHashValue(redisKey,
                "id",
                unreadMessageNum.getId(),
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 设置未读消息数
        RedisUtil.setHashValue(redisKey,
                "unreadNum",
                unreadMessageNum.getUnreadNum(),
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 设置当前会话是否打开(用户是否正处于对应聊天窗口内)
        RedisUtil.setHashValue(redisKey,
                "isOpen",
                false,
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 保存 Key
        List<String> sessionStateKeyList = new ArrayList<>();
        sessionStateKeyList.add(redisKey);
        boolean result = RedisUtil.setListValue(RedisConstant.SESSION_STATE_KEY_LIST,
                sessionStateKeyList,
                null,
                null);
        if (!result) {
            log.error(sessionStateKeyList + "缓存失败");
        }

        return unreadMessageNumCache;
    }

}




