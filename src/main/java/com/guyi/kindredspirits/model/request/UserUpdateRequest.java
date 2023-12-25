package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户请求封装类
 *
 * @author 孤诣
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 标签列，json 格式
     */
    private String tags;

    /**
     * 个人简介
     */
    private String profile;

}
