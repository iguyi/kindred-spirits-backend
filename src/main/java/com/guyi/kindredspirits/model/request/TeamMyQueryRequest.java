package com.guyi.kindredspirits.model.request;

import com.guyi.kindredspirits.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询我管理的队伍请求封装类
 *
 * @author 孤诣
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamMyQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    private static final long serialVersionUID = -1633497805936992971L;

}
