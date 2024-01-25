package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 查看好友信息请求响应封装类
 *
 * @author 孤诣
 */
@Data
public class FriendVo implements Serializable {

    /**
     * 好友 id - 在 Friend 中的 id
     */
    private Long id;

    /**
     * 之前是否是当前用户主动加的对方
     */
    private Boolean isActive;

    /**
     * 关系状态:
     * 0 - 正常好友
     * 1 - activeUserId 删除了 passiveUserId
     * 2 - passiveUserId 删除了 activeUserId
     * 3 - activeUserId 拉黑 passiveUserId
     * 4 - passiveUserId 拉黑 activeUserId
     * 说明：状态 3 可以变为 状态1; 状态 4 可以变为 状态 3
     */
    private Integer relationStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 好友信息
     */
    private UserVo friend;

    private static final long serialVersionUID = -7107921454281878895L;

}