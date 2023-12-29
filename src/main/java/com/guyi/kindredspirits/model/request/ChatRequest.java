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
     * 聊天类型
     */
    private Integer chatType;

    /**
     * 发送方 id
     */
    private Long senderId;

    /**
     * 接受方 id
     */
    private String receiverId;

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 聊天内容
     */
    private String chatContent;

    private static final long serialVersionUID = 1L;

}
