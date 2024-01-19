package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 标签消息封装类, 用于返回给前端
 *
 * @author 孤诣
 */
@Data
public class TagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 是否是父标签
     */
    private Integer isParent;

    /**
     * 父标签权值+自己的基本权值
     */
    private Integer weights;

}
