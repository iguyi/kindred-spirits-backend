package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 添加队伍的请求封装类
 *
 * @author 孤诣
 */
@Data
public class TeamAddRequest implements Serializable {

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍头像
     */
    private String avatarUrl;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    private static final long serialVersionUID = 3605169030317584853L;

}
