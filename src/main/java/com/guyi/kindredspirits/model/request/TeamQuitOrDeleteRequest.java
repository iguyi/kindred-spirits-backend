package com.guyi.kindredspirits.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 对用户退出队伍或队长删除队伍的请求参数封装类
 *
 * @author 孤诣
 */
@Data
public class TeamQuitOrDeleteRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long teamId;

    private static final long serialVersionUID = -1831963437448473166L;

}
