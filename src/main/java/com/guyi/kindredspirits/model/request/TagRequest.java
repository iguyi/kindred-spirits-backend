package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加队伍的请求封装类
 *
 * @author 张仕恒
 */
@Data
public class TagRequest implements Serializable {

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 上传者 id
     */
    private Long userId;

    /**
     * 父标签 id
     */
    private Long parentId;

    /**
     * 标签层级:
     * 0 - 一级
     * 1 - 二级
     * 2 - 三级
     */
    private Integer level;

    /**
     * 权值
     */
    private Double weights;

    private static final long serialVersionUID = 1L;
}