package com.guyi.kindredspirits.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.*;
import com.guyi.kindredspirits.model.vo.UserTeamVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 队伍接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/team")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class TempController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 创建队伍
     *
     * @param teamAddRequest - 新队伍对象
     * @return 添加队伍的数量
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTemp(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest httpServletRequest) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long tempId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(tempId);
    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest - 队伍的新信息
     * @return 队伍信息更新情况
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTemp(@RequestBody TeamUpdateRequest teamUpdateRequest,
                                            HttpServletRequest httpServletRequest) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean updateResult = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 查询指定 id 的队伍信息
     *
     * @param id - 被查询队伍的 id
     * @return 对应 id 的队伍信息; 没有查到数据, 返回原因
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        return ResultUtils.success(team);
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
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        // todo 获取登录用户和判断是否是管理员冗余了
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long loginUserId = loginUser.getId();
        boolean isAdmin = userService.isAdmin(httpServletRequest);
        List<UserTeamVo> userTeamList = teamService.listTeams(teamQuery, isAdmin);
        userTeamList.forEach(userTeamVo -> {
            List<UserVo> userList = userTeamVo.getUserList();
            if (userList != null) {
                userList.forEach(userVo -> {
                    if (loginUserId.equals(userVo.getId())) {  // 当前用户已加入该队伍
                        userTeamVo.setHasJoin(true);
                    }
                });
            }
        });
        return ResultUtils.success(userTeamList);
    }

    /**
     * 根据指定信息查询队伍
     * 筛选条件有: 队伍 id、队伍名称、队伍描述、队伍最大人数、创建人 id、队长 id、队伍状态(公开、私密、加密)
     *
     * @param teamQuery - 队伍查询封装对象
     * @return 分页返回符合要求的队伍
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQueryRequest teamQuery, HttpServletRequest httpServletRequest) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        return ResultUtils.success(teamService.listTeamsByPage(loginUser.getId(), teamQuery));
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest - 对用户加入队伍的请求消息的封装
     * @return true - 加入成功; false - 加入失败
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,
                                          HttpServletRequest httpServletRequest) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest - 对用户退出队伍的请求参数的封装
     * @return true - 退出队伍成功; false - 退出队伍失败
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitOrDeleteRequest teamQuitRequest,
                                          HttpServletRequest httpServletRequest) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     *
     * @param teamDeleteRequest - 对队长解散队伍的请求参数的封装
     * @return true - 解散队伍成功; false - 解散队伍失败
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTemp(@RequestBody TeamQuitOrDeleteRequest teamDeleteRequest,
                                            HttpServletRequest httpServletRequest) {
        if (teamDeleteRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean removeResult = teamService.deleteTeam(teamDeleteRequest, loginUser);
        if (!removeResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍解散失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取我管理的队伍
     *
     * @return 符合要求的所有队伍
     */
    @GetMapping("/list/my/leader")
    public BaseResponse<List<Team>> listMyLeaderTeams(HttpServletRequest httpServletRequest) {
        TeamMyQueryRequest teamMyQuery = new TeamMyQueryRequest();
        User loginUser = userService.getLoginUser(httpServletRequest);
        teamMyQuery.setId(loginUser.getId());
        List<Team> teamList = teamService.listMyLeaderTeams(teamMyQuery, loginUser);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @return 符合要求的所有队伍
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<Team>> listMyJoinTeams(HttpServletRequest httpServletRequest) {
        TeamMyQueryRequest teamMyQuery = new TeamMyQueryRequest();
        User loginUser = userService.getLoginUser(httpServletRequest);
        teamMyQuery.setId(loginUser.getId());
        List<Team> teamList = teamService.listMyJoinTeams(teamMyQuery, loginUser);
        return ResultUtils.success(teamList);
    }
}
