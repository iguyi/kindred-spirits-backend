package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.dto.TeamQuery;
import com.guyi.kindredspirits.model.request.TeamJoinRequest;
import com.guyi.kindredspirits.model.request.TeamQuitOrDeleteRequest;
import com.guyi.kindredspirits.model.request.TeamUpdateRequest;
import com.guyi.kindredspirits.model.vo.UserTeamVo;

import java.util.List;

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

    /**
     * 搜索队伍
     *
     * @param teamQuery - 用于查询队伍的参数的封装
     * @param isAdmin   - 是否是管理员
     * @return 用于返回给前端的用户队伍信息列表, 包括了队伍信息, 队伍成员信息
     */
    List<UserTeamVo> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest - 队伍的新信息
     * @param loginUser         - 当前登录用户
     * @return true - 更新成功; false - 更新失败
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest - 对用户加入队伍的请求消息的封装
     * @param loginUser       - 当前登录用户
     * @return true - 加入成功; false - 加入失败
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     *
     * @param teamQuitRequest - 对用户退出队伍的请求参数的封装
     * @param loginUser       - 当前登录用户
     * @return true - 退出队伍成功; false - 退出队伍失败
     */
    boolean quitTeam(TeamQuitOrDeleteRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     *
     * @param teamDeleteRequest - 对用户退出队伍的请求参数的封装
     * @return true - 解散队伍成功; false - 解散队伍失败
     */
    boolean deleteTeam(TeamQuitOrDeleteRequest teamDeleteRequest, User loginUser);
}
