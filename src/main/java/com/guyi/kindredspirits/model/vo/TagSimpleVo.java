package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 标签消息封装类, 用于返回给前端
 *
 * @author 孤诣
 */
@Data
public class TagSimpleVo implements Serializable {

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
    private Integer isPatent;

    private static final long serialVersionUID = -5679702908750667983L;

}
