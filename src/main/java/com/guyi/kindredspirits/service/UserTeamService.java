package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.UserTeam;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 张仕恒
* @description 针对表【user_team(用户-队伍关系表)】的数据库操作Service
* @createDate 2023-10-01 21:57:09
*/
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 根据用户 id 查询 “用户-队伍” 信息
     *
     * @param userId - 用户 id
     * @return userId 对应的 “用户-队伍“ 信息列表
     */
    List<UserTeam> getMessageByUserId(Long userId);

}
