package com.guyi.kindredspirits.model.request;

import com.guyi.kindredspirits.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 队伍查询封装类
 *
 * @author 张仕恒
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 搜索词, 在队伍名称和队伍描述中进行搜索
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 创建人 id
     */
    private Long userId;

    /**
     * 队长 id
     */
    private Long leaderId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
