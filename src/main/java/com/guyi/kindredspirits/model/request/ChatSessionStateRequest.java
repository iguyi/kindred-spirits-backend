package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 设置聊天会话状态请求
 *
 * @author 孤诣
 */
@Data
public class ChatSessionStateRequest implements Serializable {

    /**
     * 好友或队伍 id
     */
    private Long id;

    /**
     * 聊天会话的类型
     *
     * @see com.guyi.kindredspirits.common.enums.ChatTypeEnum
     */
    private Integer chatType;

    /**
     * 聊天会话状态, 聊天会话打开表示用户正在对应聊天窗口内<br/>
     * - true: 聊天会话打开<br/>
     * - false: 聊天会话关闭
     */
    private Boolean state;

    private static final long serialVersionUID = 2903937319984483386L;

}
