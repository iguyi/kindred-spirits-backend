package com.guyi.kindredspirits.model.vo;

import lombok.Data;

/**
 * 用户 WebSocket 签名
 *
 * @author 孤诣
 */
@Data
public class WebSocketVo {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

}
