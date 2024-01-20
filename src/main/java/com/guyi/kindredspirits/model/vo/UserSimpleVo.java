package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户最基本
 *
 * @author 孤诣
 */
@Data
public class UserSimpleVo implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

    private static final long serialVersionUID = 7738566377130226836L;

}