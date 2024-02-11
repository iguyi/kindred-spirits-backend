package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.cache.UnreadMessageNumCache;
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


    /**
     * 更新未读消息数
     *
     * @param unreadNum   - 未读数
     * @param sessionName - 会话名称
     * @return 更新结果
     */
    boolean updateUnreadMessageNum(String sessionName, int unreadNum);

    /**
     * 根据会话名称获取 unread_message_num 对应的缓存实体类
     *
     * @param sessionName - 会话名称
     * @return UnreadMessageNumCache
     */
    UnreadMessageNumCache getUnreadMessageNumByName(String sessionName);

}
