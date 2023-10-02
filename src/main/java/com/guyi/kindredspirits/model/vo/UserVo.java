package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户包装类(脱敏)
 */
@Data
public class UserVo implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户身份
     */
    private Integer userRole;

    /**
     * 标签列，json 格式
     */
    private String tags;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 状态 0 - 正常
     */
    private Integer userStatus;

    /**
     * 是否为热点用户 0-不是 1-是
     */
    private Integer isHot;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}