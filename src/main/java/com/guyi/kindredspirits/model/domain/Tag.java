package com.guyi.kindredspirits.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签表
 *
 * @author 孤诣
 */
@TableName(value = "tag")
@Data
public class Tag implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 7853696643171338748L;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签创建者 id
     */
    private Long userId;

    /**
     * 是否是父标签
     */
    private Integer isParent;

    /**
     * 父标签 id
     */
    private Long parentId;

    /**
     * 基本权值
     */
    private Integer baseWeight;

    /**
     * 父标签权值+自己的基本权值
     */
    private Integer weights;

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

}