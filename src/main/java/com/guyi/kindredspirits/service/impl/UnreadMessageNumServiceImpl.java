package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatSessionStateRequest;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.mapper.UnreadMessageNumMapper;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import org.springframework.stereotype.Service;

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
    private static final Long SESSION_STATE_EXPIRATION = 10L;

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
        String redisKey = String.format(RedisConstant.KEY_PRE, "message", "unread", chatSessionName);

        if (RedisUtil.hasRedisKey(redisKey)) {
            // 对应会话的数据在缓存中存在, 直接改变对应状态即可
            RedisUtil.setHashValue(redisKey,
                    "isOpen",
                    Optional.ofNullable(state).orElse(false),
                    SESSION_STATE_EXPIRATION,
                    SESSION_STATE_EXPIRATION_UNIT);
            return;
        }

        // 数据库查询会话的数据
        QueryWrapper<UnreadMessageNum> unreadMessageNumQueryWrapper = new QueryWrapper<>();
        unreadMessageNumQueryWrapper.select("id", "unreadNum")
                .eq("userId", loginUserId)
                .eq("chatSessionName", redisKey);
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
                "unread",
                Optional.ofNullable(target.getUnreadNum()).orElse(0),
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );

        // 设置当前会话是否打开(用户是否正处于对应聊天窗口内)
        RedisUtil.setHashValue(redisKey,
                "isOpen",
                Optional.ofNullable(state).orElse(false),
                SESSION_STATE_EXPIRATION,
                SESSION_STATE_EXPIRATION_UNIT
        );
    }

}



