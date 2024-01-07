package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.UserTeam;

import java.util.List;

/**
 * 针对表 user_team(用户-队伍关系表) 的数据库操作 Service
 *
 * @author 孤诣
 */
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 根据用户 id 查询 "用户-队伍" 信息
     *
     * @param userId - 用户 id
     * @return userId 对应的 “用户-队伍“ 信息列表
     */
    List<UserTeam> getMessageByUserId(Long userId);

    /**
     * 根据队伍 id 查询 "用户-队伍" 信息
     *
     * @param teamId - 队伍 id
     * @return teamId 对应的 “用户-队伍“ 信息列表
     */
    List<UserTeam> getMessageByTeamId(Long teamId);

}
