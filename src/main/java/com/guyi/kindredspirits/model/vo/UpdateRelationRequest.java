package com.guyi.kindredspirits.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新好友状态请求封装类
 *
 * @author 孤诣
 */
@Data
public class UpdateRelationRequest implements Serializable {

    /**
     * 好友 id
     */
    private Long friendId;

    /**
     * 好友关系状态
     */
    private Integer relationStatus;

    private static final long serialVersionUID = 8311275218886581143L;

}
