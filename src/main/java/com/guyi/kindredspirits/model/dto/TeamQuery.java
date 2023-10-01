package com.guyi.kindredspirits.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.guyi.kindredspirits.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 队伍查询封装类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

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
