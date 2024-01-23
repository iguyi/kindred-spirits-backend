package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 队长将成员踢出队伍请求封装类
 *
 * @author 孤诣
 */
@Data
public class KickOutRequest implements Serializable {

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
