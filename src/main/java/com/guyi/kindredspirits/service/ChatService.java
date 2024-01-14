package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatHistoryRequest;
import com.guyi.kindredspirits.model.vo.ChatVo;

import java.util.List;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Service
 *
 * @author 孤诣
 */
public interface ChatService extends IService<Chat> {

    /**
     * 获取 "聊天请求响" 对象
     *
     * @param senderUser   - 消息发送者
     * @param receiverUser - 消息接收者
     * @param chatContent  - 消息内容
     * @param chatTypeEnum - 消息类型枚举
     * @return "聊天请求响应" 对象
     */
    ChatVo getChatVo(User senderUser, User receiverUser, String chatContent, ChatTypeEnum chatTypeEnum);

    /**
     * 获取私聊室的历史聊天记录
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     * @return 私聊室的历史聊天记录列表
     */
    List<ChatVo> getPrivateChat(ChatHistoryRequest chatHistoryRequest);

    /**
     * 获取队伍聊天室的历史聊天记录
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     * @return 队伍聊天室的历史聊天记录列表
     */
    List<ChatVo> getTeamChat(ChatHistoryRequest chatHistoryRequest);
}
