package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 加入队伍的请求封装类
 *
 * @author 孤诣
 */
@Data
public class TeamJoinRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;

    /**
     * 队伍邀请链接
     */
    private String teamLink;

    private static final long serialVersionUID = 1769513560162057479L;

}
