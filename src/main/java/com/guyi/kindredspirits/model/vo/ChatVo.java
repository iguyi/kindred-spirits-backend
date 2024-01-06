package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 聊天请求响应封装类
 *
 * @author 孤诣
 */
@Data
public class ChatVo {

    /**
     * 消息发送者
     */
    private WebSocketVo senderUser;

    /**
     * 消息接收者
     */
    private WebSocketVo receiverUser;

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 聊天内容
     */
    private String chatContent;

    /**
     * 聊天类型
     */
    private Integer chatType;

    /**
     * 发送时间, 格式为: yyyy-MM-dd HH:mm:ss
     */
    private String sendTime;

    private static final long serialVersionUID = 1L;

}
