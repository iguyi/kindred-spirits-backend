package com.guyi.kindredspirits.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.*;
import com.guyi.kindredspirits.model.vo.TeamAllVo;
import com.guyi.kindredspirits.model.vo.TeamVo;
import com.guyi.kindredspirits.model.vo.UserTeamVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 创建队伍
     *
     * @param teamAddRequest - 新队伍对象
     * @return 添加队伍的数量
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTemp(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 创建队伍
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long tempId = teamService.addTeam(team, loginUser);

        return ResultUtils.success(tempId);
    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest  - 队伍的新信息
     * @param httpServletRequest - 客户端请求
     * @return 队伍信息更新情况
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTemp(@RequestBody TeamUpdateRequest teamUpdateRequest,
                                            HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 更新队伍
        boolean updateResult = teamService.updateTeam(teamUpdateRequest, loginUser);

        if (updateResult) {
            return ResultUtils.success(true);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "队伍更新失败");
    }

    /**
     * 查询指定 id 的队伍信息
     *
     * @param id - 被查询队伍的 id
     * @return 对应 id 的队伍信息; 没有查到数据, 返回原因
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        Team team = teamService.getById(id);
        if (team != null) {
            return ResultUtils.success(team);
        }

        return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
    }

    /**
     * 自由搜索队伍 - 主要用于用户查找队伍
     *
     * @param searchCondition    - 搜索条件(关键词)
     * @param pageSize-          每页数据量大小, (0, 20]
     * @param pageNum            - 页码, >0
     * @param httpServletRequest - 客户端请求
     * @return 符合要求的队伍
     */
    @GetMapping("/search")
    public BaseResponse<List<TeamVo>> searchTeam(String searchCondition, long pageSize, long pageNum,
                                                 HttpServletRequest httpServletRequest) {
        // 参数校验
        userService.getLoginUser(httpServletRequest);
        if (StringUtils.isBlank(searchCondition)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        List<Team> teamList = teamService.searchTeam(searchCondition, pageSize, pageNum);
        List<TeamVo> result = teamList.stream().map(team -> {
            TeamVo teamVo = new TeamVo();
            BeanUtils.copyProperties(team, teamVo);
            return teamVo;
        }).collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    /**
     * 查看自己队伍的详细信息
     *
     * @param teamId             - 队伍 id
     * @param httpServletRequest - 客户端请求
     * @return 队伍详细信息
     */
    @GetMapping("/check")
    public BaseResponse<TeamAllVo> checkTeam(Long teamId, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        return ResultUtils.success(teamService.checkTeam(loginUser, teamId));
    }

    /**
     * 根据指定信息查询队伍。<br/>
     * 筛选条件有: 队伍 id、队伍名称、队伍描述、队伍最大人数、创建人 id、队长 id、队伍状态(公开、私密、加密)。<br/>
     *
     * @param teamQuery - 队伍查询封装对象。
     * @return 符合要求的所有队伍, 返回值中有一个 hasJoin 字段, 用户判断当前用户是否已加入队伍。<br/>
     * 注意:
     * (1) 前端不应该将 hasJoin = true 的数据展示;<br/>
     * (2) 如果需要展示当前已加入的队伍, 请前使用 this.listMyJoinTeams() 接口.
     */
    @GetMapping("/list")
    public BaseResponse<List<UserTeamVo>> listTeams(TeamQueryRequest teamQuery, HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        // 获取符合要求的数据
        boolean isAdmin = userService.isAdmin(httpServletRequest);
        List<UserTeamVo> userTeamList = teamService.listTeams(teamQuery, isAdmin);

        // 过滤已加入的队伍
        Long loginUserId = loginUser.getId();
        userTeamList.forEach(userTeamVo -> {
            List<UserVo> userList = userTeamVo.getUserList();
            if (userList != null) {
                userList.forEach(userVo -> {
                    if (loginUserId.equals(userVo.getId())) {
                        userTeamVo.setHasJoin(true);
                    }
                });
            }
        });

        // 返回数据
        return ResultUtils.success(userTeamList);
    }

    /**
     * 根据指定信息查询队伍<br/>
     * 筛选条件有: 队伍 id、队伍名称、队伍描述、队伍最大人数、创建人 id、队长 id、队伍状态(公开、私密、加密)
     *
     * @param teamQuery          - 队伍查询封装对象
     * @param httpServletRequest - 客户端请求
     * @return 分页返回符合要求的队伍
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQueryRequest teamQuery, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        return ResultUtils.success(teamService.listTeamsByPage(loginUser.getId(), teamQuery));
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest    - 对用户加入队伍的请求消息的封装
     * @param httpServletRequest - 客户端请求
     * @return true - 加入成功; false - 加入失败
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,
                                          HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据队伍邀请码加入队伍
     *
     * @param teamJoinRequest    - 入队请求封装
     * @param httpServletRequest - 客户端请求
     * @return 操作结果
     */
    @PostMapping("/join/link")
    public BaseResponse<Boolean> joinTeamByLink(@RequestBody TeamJoinRequest teamJoinRequest,
                                                HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Boolean result = teamService.joinTeamByLink(teamJoinRequest, loginUser);
        if (result != null && result) {
            return ResultUtils.success(true);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统繁忙");
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest    - 对用户退出队伍的请求参数的封装
     * @param httpServletRequest - 客户端请求
     * @return true - 退出队伍成功; false - 退出队伍失败
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitOrDeleteRequest teamQuitRequest,
                                          HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     *
     * @param teamDeleteRequest  - 对队长解散队伍的请求参数的封装
     * @param httpServletRequest - 客户端请求
     * @return true - 解散队伍成功; false - 解散队伍失败
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTemp(@RequestBody TeamQuitOrDeleteRequest teamDeleteRequest,
                                            HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamDeleteRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        boolean removeResult = teamService.deleteTeam(teamDeleteRequest, loginUser);
        if (removeResult) {
            return ResultUtils.success(true);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "队伍解散失败");
    }

    /**
     * 获取我管理的队伍
     *
     * @param httpServletRequest - 客户端请求
     * @return 符合要求的所有队伍
     */
    @GetMapping("/list/my/leader")
    public BaseResponse<List<Team>> listMyLeaderTeams(HttpServletRequest httpServletRequest) {
        // 登录校验
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 获取数据
        TeamMyQueryRequest teamMyQuery = new TeamMyQueryRequest();
        teamMyQuery.setId(loginUser.getId());
        List<Team> teamList = teamService.listMyLeaderTeams(teamMyQuery, loginUser);

        // 响应
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @param httpServletRequest - 客户端请求
     * @return 符合要求的所有队伍
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<Team>> listMyJoinTeams(HttpServletRequest httpServletRequest) {
        // 登录校验
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 获取数据
        TeamMyQueryRequest teamMyQuery = new TeamMyQueryRequest();
        teamMyQuery.setId(loginUser.getId());
        List<Team> teamList = teamService.listMyJoinTeams(teamMyQuery, loginUser);

        // 响应
        return ResultUtils.success(teamList);
    }

    /**
     * 将指定成员提出队伍
     *
     * @param operationMemberRequest - 队长将成员踢出队伍请求封装类对象
     * @param httpServletRequest     - 客户端请求
     * @return 操作结果
     */
    @PostMapping("/kick")
    public BaseResponse<Boolean> kickOut(@RequestBody OperationMemberRequest operationMemberRequest,
                                         HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (operationMemberRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        return ResultUtils.success(teamService.kickOut(loginUser, operationMemberRequest));
    }

    /**
     * 队长位置转让
     *
     * @param operationMemberRequest - 队长位置转让请求封装类
     * @param httpServletRequest     - 客户端请求
     * @return 操作结果
     */
    @PostMapping("/abdicator")
    public BaseResponse<Boolean> abdicator(@RequestBody OperationMemberRequest operationMemberRequest,
                                           HttpServletRequest httpServletRequest) {
        // 参数校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (operationMemberRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        return ResultUtils.success(teamService.abdicator(loginUser, operationMemberRequest));
    }

    /**
     * 刷新入队链接
     *
     * @param teamId             - 队伍 id
     * @param httpServletRequest - 客户端请求
     * @return 队伍的新入队链接
     */
    @GetMapping("/link")
    public BaseResponse<String> refreshLink(Long teamId, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (teamId == null || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        return ResultUtils.success(teamService.refreshLink(loginUser, teamId));
    }

}
