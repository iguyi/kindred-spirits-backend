package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类(脱敏), 用于将数据返回给前端
 *
 * @author 孤诣
 */
@Data
public class UserTeamVo implements Serializable {
    /**
     * id
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
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人 id
     */
    private Long userId;

    /**
     * 队长 id
     */
    private Long leaderId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 队伍成员列表
     */
    private List<UserVo> userList;

    /**
     * 是否已加入队伍, 默认未加入
     */
    private boolean hasJoin = false;

    private static final long serialVersionUID = 1L;
}
