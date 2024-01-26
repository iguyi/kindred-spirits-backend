package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 处理好友申请请求封装
 *
 * @author 孤诣
 */
@Data
public class ProcessFriendApplyRequest implements Serializable {

    /**
     * 发送好友请求者 id
     */
    private Long senderId;

    /**
     * 是否同意 senderId 的好友申请
     */
    private Boolean isAgreed;

    private static final long serialVersionUID = -4365895923466354812L;

}
