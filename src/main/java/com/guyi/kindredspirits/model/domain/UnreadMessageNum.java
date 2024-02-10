package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 未读聊天记录统计表
 *
 * @author 孤诣
 */
@TableName(value = "unread_message_num")
@Data
public class UnreadMessageNum implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * id
     */
    private Long userId;

    /**
     * 聊天会话名称: 会话类型-用户id-好友/队伍id
     */
    private String chatSessionName;

    /**
     * 未读消息数
     */
    private Integer unreadNum;

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
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 7532056979462854291L;

}