package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建标签请求封装类
 *
 * @author 孤诣
 */
@Data
public class TagAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private Integer isPatent;

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

}