package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取最近的聊天会话请求响应封装
 *
 * @author 孤诣
 */
@Data
public class ChatRoomVo implements Serializable {

    /**
     * 聊天室消息接收者(朋友或队伍) id
     */
    private Long receiverId;

    /**
     * 接收者名称(好友名或队伍名)
     */
    private String receiverName;

    /**
     * 聊天室消息接收者(朋友或队伍) 头像
     */
    private String avatarUrl;

    /**
     * 聊天室类型:
     * - 1 -> 私聊
     * - 2 -> 群聊
     */
    private Integer chatType;

    /**
     * 最后的聊天记录消息
     * - 私聊: "消息"
     * - 群聊:
     * -- 其他人： "消息发送人昵称: 消息"
     * -- 自己: "消息"
     */
    private String lastRecord;

    /**
     * 发送时间
     */
    private String sendTime;

    private static final long serialVersionUID = 4910198001360086136L;

}
