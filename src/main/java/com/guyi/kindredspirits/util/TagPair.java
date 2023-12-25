package com.guyi.kindredspirits.util;

import lombok.Data;

import java.io.Serializable;

/**
 * 存储标签的名称和权值
 *
 * @author 孤诣
 */
@Data
public class TagPair implements Serializable {
    private static final long serialVersionUID = 1;
    private final String tag;
    private final Integer weights;
}
