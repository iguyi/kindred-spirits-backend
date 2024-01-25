package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 队长操作队伍成员请求封装类
 * - 位置转让
 * - 踢出成员
 *
 * @author 孤诣
 */
@Data
public class OperationMemberRequest implements Serializable {

    /**
     * 成员 id
     */
    private Long memberId;

    /**
     * 队伍 id
     */
    private Long teamId;

    private static final long serialVersionUID = -9047945341054040769L;

}
