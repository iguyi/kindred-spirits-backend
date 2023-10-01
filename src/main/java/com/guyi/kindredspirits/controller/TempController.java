package com.guyi.kindredspirits.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.dto.TeamQuery;
import com.guyi.kindredspirits.model.request.TeamAddRequest;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 队伍接口
 */
@RestController
@RequestMapping("/team")
@Slf4j
@CrossOrigin(value = {"http://127.0.0.1:8081", "http://localhost:8081"})
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
     * 解散队伍
     *
     * @param id - 被解散队伍的 id
     * @return 队伍解散情况
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTemp(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // todo 鉴权: 用户是否登录？
        // todo 删除队伍的人是否是队长？
        boolean removeResult = teamService.removeById(id);
        if (!removeResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍解散失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新队伍信息
     *
     * @param team - 队伍的新信息
     * @return 队伍信息更新情况
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTemp(@RequestBody Team team) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // todo 鉴权: 用户是否登录？
        // todo 删除队伍的人是否是队长？
        boolean updateResult = teamService.updateById(team);
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
     * 根据指定信息查询队伍
     * 筛选条件有: 队伍 id、队伍名称、队伍描述、队伍最大人数、创建人 id、队长 id、队伍状态(公开、私密、加密)
     *
     * @param teamQuery - 队伍查询封装对象
     * @return 符合要求的所有队伍
     */
    @GetMapping("/list")
    public BaseResponse<List<Team>> listTeams(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        List<Team> teamList = teamService.list(teamQueryWrapper);
        return ResultUtils.success(teamList);
    }

    /**
     * 根据指定信息查询队伍
     * 筛选条件有: 队伍 id、队伍名称、队伍描述、队伍最大人数、创建人 id、队长 id、队伍状态(公开、私密、加密)
     *
     * @param teamQuery - 队伍查询封装对象
     * @return 分页返回符合要求的队伍
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        Page<Team> teamPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        Page<Team> resultPage = teamService.page(teamPage, teamQueryWrapper);
        return ResultUtils.success(resultPage);
    }
}
