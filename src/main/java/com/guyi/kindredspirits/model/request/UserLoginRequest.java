package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author 张仕恒
 */
@Data  // 可以生成 set 和 get
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
