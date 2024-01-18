package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 队伍表
 *
 * @author 孤诣
 */
@TableName(value = "team")
@Data
public class Team implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 队伍已有人数
     */
    private Integer num;

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
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = -3890199569607069722L;

}