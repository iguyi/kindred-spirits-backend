package com.guyi.kindredspirits.model.request;

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
     * 操作: 1-表示删除 2-表示拉黑
     */
    private Integer operation;

    /**
     * 之前是否是当前用户主动添加的对方
     */
    private Boolean isActive;

    private static final long serialVersionUID = 8311275218886581143L;

}
