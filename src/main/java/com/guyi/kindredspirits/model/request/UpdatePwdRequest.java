package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求封装类
 *
 * @author 孤诣
 */
@Data
public class UpdatePwdRequest implements Serializable {

    /**
     * 原密码
     */
    private String oldPwd;

    /**
     * 新密码
     */
    private String newPwd;

    /**
     * 确认密码
     */
    private String checkPwd;

    private static final long serialVersionUID = -7815188836069188139L;

}
