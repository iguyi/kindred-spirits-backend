package com.guyi.kindredspirits.mapper;

import com.guyi.kindredspirits.model.domain.Chat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Mapper
 *
 * @author 孤诣
 */
public interface ChatMapper extends BaseMapper<Chat> {

    /**
     * 统计指定队伍中的聊天记录数
     *
     * @param teamId - 队伍 id
     * @return 和指定队伍相关的聊天记录数
     */
    int countByTeamId(@Param("teamId") Long teamId);

}




