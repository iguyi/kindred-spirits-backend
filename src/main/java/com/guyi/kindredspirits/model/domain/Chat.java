package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 聊天记录表
 *
 * @author 孤诣
 */
@TableName(value = "chat")
@Data
public class Chat implements Serializable {

    /**
     * 聊天记录 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息发送者 id
     */
    private Long senderId;

    /**
     * 消息接收者 id
     */
    private Long receiverId;

    /**
     * 群聊时, 对应队伍的 id
     */
    private Long teamId;

    /**
     * teamId 对应队伍的成员的 id 列表, 格式为 JSON
     */
    private String receiverIds;

    /**
     * 聊天内容
     */
    private String chatContent;

    /**
     * 聊天类型 1-私聊 2-群聊
     */
    private Integer chatType;

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
    private static final long serialVersionUID = 1L;

}