package com.guyi.kindredspirits.model.vo;

import java.util.Date;

/**
 * 聊天请求响应封装类
 *
 * @author 孤诣
 */
public class ChatVo {

    /**
     * 消息发送者
     */
    private UserVo senderUserVo;

    /**
     * 消息接收者
     */
    private UserVo receiverUserVo;

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
     * 发送时间
     */
    private Date sendTime;

    private static final long serialVersionUID = 1L;

}
