package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 获取消息列表响应封装类
 *
 * @author 孤诣
 */
@Data
public class MessageVo implements Serializable {

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

    /**
     * 消息是否已处理: 0-未处理, 1-已处理
     */
    private Integer processed;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = -4280330258936892039L;

}