package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author 孤诣
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3046761170230231032L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
