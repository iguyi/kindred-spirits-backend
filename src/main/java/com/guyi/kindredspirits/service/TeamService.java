package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;

/**
 * @author 张仕恒
 * @description 针对表【team(队伍表)】的数据库操作Service
 * @createDate 2023-10-01 21:54:58
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team      - 新队伍对象
     * @param loginUser - 当前登录用户
     * @return 添加队伍的数量
     */
    long addTeam(Team team, User loginUser);

}
