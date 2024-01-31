package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新队伍的请求封装类
 *
 * @author 孤诣
 */
@Data
public class TeamUpdateRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

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

    private static final long serialVersionUID = 1145460880112951333L;

}
