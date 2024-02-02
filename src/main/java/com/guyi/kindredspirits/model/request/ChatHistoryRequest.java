package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取聊天记录请求封装
 *
 * @author 孤诣
 */
@Data
public class ChatHistoryRequest implements Serializable {

    /**
     * 私聊时, 对应好友的 id
     */
    private Long friendId;

    /**
     * 群聊时, 对应队伍的 id
     */
    private Long teamId;

    private static final long serialVersionUID = -7425907527382387336L;

}
