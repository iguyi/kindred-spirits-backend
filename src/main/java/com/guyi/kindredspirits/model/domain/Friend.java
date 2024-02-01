package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 好友表
 *
 * @author 孤诣
 */
@TableName(value = "friend")
@Data
public class Friend implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * activeUser 向 passiveUser 发出好友申请
     */
    private Long activeUserId;

    /**
     * passiveUser 同意 activeUser 的好友申请
     */
    private Long passiveUserId;

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
     * 逻辑删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 4820980863292024384L;

}