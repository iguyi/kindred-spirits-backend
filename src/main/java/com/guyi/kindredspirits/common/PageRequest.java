package com.guyi.kindredspirits.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 *
 * @author 孤诣
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 页码
     */
    protected int pageNum = 1;

    private static final long serialVersionUID = 4724111499141793926L;

}
