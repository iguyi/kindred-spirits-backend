package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息封装类
 *
 * @author 孤诣
 */
@Data
public class MessageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息发送者 id
     */
    private Long senderId;

    /**
     * 消息接受者 id
     */
    private Long receiverId;

    /**
     * 消息类型:
     * 0 - 系统消息(好友申请通过、入队申请通过等)
     * 1 - 验证消息(比如好友申请、入队申请等)
     * 2 - 消息通知(需要系统处理的消息)
     */
    private Integer messageType;

    /**
     * 消息主体(内容)
     */
    private String messageBody;

    private static final long serialVersionUID = 5898698196341759649L;

}