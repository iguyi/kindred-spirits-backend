package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.model.domain.Chat;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.vo.ChatVo;

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

}
