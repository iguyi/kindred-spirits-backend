package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatSessionStateRequest;

/**
 * 针对表 unread_message_num(未读聊天记录统计表) 的数据库操作 Service
 *
 * @author 孤诣
 */
public interface UnreadMessageNumService extends IService<UnreadMessageNum> {

    /**
     * 设置聊天会话状态请求
     *
     * @param loginUser    - 当前登录用户
     * @param stateRequest - 设置聊天会话状态请求参数的封装
     */
    void setSessionSate(User loginUser, ChatSessionStateRequest stateRequest);

}
