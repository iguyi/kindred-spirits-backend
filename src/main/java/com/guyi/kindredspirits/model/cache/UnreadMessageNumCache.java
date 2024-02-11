package com.guyi.kindredspirits.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * unread_message_num 表对应的缓存的实体类
 *
 * @author 孤诣
 */
@Data
public class UnreadMessageNumCache implements Serializable {

    /**
     * 未读消息统计信息的 id, 对应 unread_message_num.id
     */
    private Long id;

    /**
     * 未读消息数
     */
    private Integer unreadNum;

    /**
     * 对应会话窗口的状态
     */
    private Boolean isOpen;

    private static final long serialVersionUID = -4102374709990289293L;

}
