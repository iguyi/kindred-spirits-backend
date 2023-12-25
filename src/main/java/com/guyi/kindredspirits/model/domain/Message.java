package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 消息表
 *
 * @author 孤诣
 */
@TableName(value = "message")
@Data
public class Message implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息发送者 id
     */
    private Long senderId;

    /**
     * 消息接受者 id
     */
    private Long receiverId;

    /**
     * 消息类型:
     * 0 - 系统消息(好友申请通过、入队申请通过等)
     * 1 - 验证消息(比如好友申请、入队申请等)
     * 2 - 消息通知(需要系统处理的消息)
     */
    private Integer messageType;

    /**
     * 消息主体(内容)
     */
    private String messageBody;

    /**
     * 消息是否已处理: 0-未处理, 1-已处理(不需要处理的消息，值为 NaN)
     */
    private Integer processed;

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