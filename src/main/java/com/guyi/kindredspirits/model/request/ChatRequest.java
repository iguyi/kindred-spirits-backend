package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天请求
 *
 * @author 孤诣
 */
@Data
public class ChatRequest implements Serializable {

    /**
     * 消息发送者 id
     */
    private Long senderId;

    /**
     * 消息接收者 id
     */
    private Long receiverId;

    /**
     * 群聊时, 对应队伍的 id
     */
    private Long teamId;

    /**
     * 聊天内容
     */
    private String chatContent;

    /**
     * 聊天类型 1-私聊 2-群聊
     */
    private Integer chatType;

    private static final long serialVersionUID = 1L;

}
