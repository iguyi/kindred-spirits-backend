package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.TeamMapper;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.model.dto.TeamMyQuery;
import com.guyi.kindredspirits.model.dto.TeamQuery;
import com.guyi.kindredspirits.model.enums.TeamStatusEnum;
import com.guyi.kindredspirits.model.request.TeamJoinRequest;
import com.guyi.kindredspirits.model.request.TeamQuitOrDeleteRequest;
import com.guyi.kindredspirits.model.request.TeamUpdateRequest;
import com.guyi.kindredspirits.model.vo.UserTeamVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 张仕恒
 * @description 针对表【team(队伍表)】的数据库操作Service实现
 * @createDate 2023-10-01 21:54:58
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    /**
     * 创建队伍
     *
     * @param team      - 新队伍对象
     * @param loginUser - 当前登录用户
     * @return 添加队伍的数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 是否登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        final Long userId = loginUser.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录用户参数错误");
        }
        //  队伍人数校验
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求!");
        }
        //  队伍标题长度验证
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不符合要求!");
        }
        //  队伍描述验证
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不符合要求!");
        }
        //  是否公开，不传默认 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不符合要求!");
        }
        //  是否是加密的队伍
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            // 密码必须有，且长度 <= 32
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置错误!");
            }
        }
        //  超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间设置错误!");
        }
        //  校验用户以加入/创建的队伍不超过 5 个
        // todo 同步
        QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", userId);
        long hasTeamNum = userTeamService.count(teamQueryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属队伍不能大于 5 个!");
        }
        //  插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        team.setLeaderId(userId);
        boolean saveResult = this.save(team);
        Long teamId = team.getId();  // 插入成功的话, id 会回显
        if (!saveResult || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍创建失败!");
        }
        //  插入数据到 用户-队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        saveResult = userTeamService.save(userTeam);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍创建失败!");
        }
        return 0;
    }

    /**
     * 搜索队伍
     *
     * @param teamQuery - 用于查询队伍的参数的封装
     * @param isAdmin   - 是否是管理员
     * @return 用于返回给前端的用户队伍信息列表, 包括了队伍信息, 队伍成员信息
     */
    @Override
    public List<UserTeamVo> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                teamQueryWrapper.eq("id", id);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                teamQueryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                teamQueryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0 && maxNum <= 20) {
                teamQueryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                teamQueryWrapper.eq("userId", userId);
            }
            Long leaderId = teamQuery.getLeaderId();
            if (leaderId != null && leaderId > 0) {
                teamQueryWrapper.eq("leaderId", leaderId);
            }
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;  // 默认查询公开的队伍
            }
            //  非公开队伍需要管理员权限才能查询
            if (!isAdmin && !TeamStatusEnum.PUBLIC.equals(statusEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "查询失败");
            }
            teamQueryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期队伍
        teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(teamQueryWrapper);
        // 关联查询队伍查询信息
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        // todo 需要改成队伍所有成员
        // 关联查询创建人信息
        List<UserTeamVo> userTeamVoList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            // todo 根据队伍id 查询 user_team, 拿到成员 id, 到 user 表查询成员信息(建议 SQL 方式)
            User user = userService.getById(userId);
            // 脱敏
            UserTeamVo userTeamVo = new UserTeamVo();
            BeanUtils.copyProperties(team, userTeamVo);
            if (user != null) {
                UserVo userVo = new UserVo();
                BeanUtils.copyProperties(user, userVo);
                List<UserVo> userVoList = new ArrayList<>();
                userVoList.add(userVo);
                userTeamVo.setUserList(userVoList);
            }
            userTeamVoList.add(userTeamVo);
        }
        return userTeamVoList;
    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest - 队伍的新信息
     * @param loginUser         - 当前登录用户
     * @return true - 更新成功; false - 更新失败
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        Long loginUserId = loginUser.getId();
        Long leaderId = oldTeam.getLeaderId();
        // 不是队长, 也不是管理员, 拒绝操作
        if (!leaderId.equals(loginUserId) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        // todo 新值与旧值一致就不操作数据库
        Integer newTeamStatus = teamUpdateRequest.getStatus();
        TeamStatusEnum newEnumTeamStatus = TeamStatusEnum.getEnumByValue(newTeamStatus);
        if (TeamStatusEnum.SECRET.equals(newEnumTeamStatus)) {  // 修改为加密队伍
            Integer oldTeamStatus = oldTeam.getStatus();
            TeamStatusEnum oldEnumTeamStatus = TeamStatusEnum.getEnumByValue(oldTeamStatus);
            String oldPassword = oldTeam.getPassword();
            if (!newEnumTeamStatus.equals(oldEnumTeamStatus)) {  // 原队伍不是加密的
                if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须要有密码");
                }
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest - 对用户加入队伍的请求消息的封装
     * @param loginUser       - 当前登录用户
     * @return true - 加入成功; false - 加入失败
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求数据为空");
        }
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求数据为空");
        }
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求数据为空");
        }
        // 用户所属队伍数量校验
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", loginUserId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if (userTeamList.size() >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属队伍不能操作 5 个");
        }
        for (UserTeam userTeam : userTeamList) {
            if (userTeam.getTeamId().equals(teamId)) {  // 幂等性
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
            }
        }
        // 队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        // 队伍是否过期
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 队伍状态判断
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "入队密码错误");
            }
        }
        // 队伍人数校验
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }
        // 新增用户-队伍关联信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUserId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }

    /**
     * 用户退出队伍
     *
     * @param teamQuitRequest - 对用户退出队伍的请求参数的封装
     * @param loginUser       - 当前登录用户
     * @return true - 退出队伍成功; false - 退出队伍失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitOrDeleteRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求数据为空");
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(loginUserId);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        // 队伍人数校验
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) { // 队伍只有 1 人
            // 删除队伍相关信息
            boolean removeTeamResult = this.removeById(teamId);
            if (!removeTeamResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
            }
            userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId", teamId);
            boolean removeUserTeamResult = userTeamService.removeById(userTeamQueryWrapper);
            if (!removeUserTeamResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
            }
            return true;
        } else {
            if (loginUserId.equals(team.getLeaderId())) {  // 队长不能退出
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队长不能退出队伍");
            }
            userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("userId", loginUserId);
            userTeamQueryWrapper.eq("teamId", teamId);
            return userTeamService.remove(userTeamQueryWrapper);
        }
    }

    /**
     * 解散队伍
     *
     * @param teamDeleteRequest - 对用户退出队伍的请求参数的封装
     * @return true - 解散队伍成功; false - 解散队伍失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(TeamQuitOrDeleteRequest teamDeleteRequest, User loginUser) {
        // 用户登录是否正确
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求数据为空");
        }
        if (teamDeleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 判断队伍是否存在
        Long teamId = teamDeleteRequest.getTeamId();
        Team team = getTeamById(teamId);
        if (!loginUserId.equals(team.getLeaderId())) {  // 不是队长, 无权限
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长可以解散队伍");
        }
        // 删除队伍相关信息
        boolean removeTeamResult = this.removeById(teamId);
        if (!removeTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean removeUserTeamResult = userTeamService.removeById(userTeamQueryWrapper);
        if (!removeUserTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
        }
        return true;
    }

    /**
     * 获取我管理的队伍
     *
     * @param teamMyQuery - 查询我管理的队伍请求封装对象
     * @param loginUser - 当前登录用户
     * @return 符合要求的所有队伍
     */
    @Override
    public List<Team> listMyLeaderTeams(TeamMyQuery teamMyQuery, User loginUser) {
        if (teamMyQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        Long leaderId = teamMyQuery.getId();
        Long loginUserId = loginUser.getId();
        if (leaderId == null || leaderId <= 0 || loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        if (!leaderId.equals(loginUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("leaderId", leaderId);
        return this.list(teamQueryWrapper);
    }

    /**
     * 获取我加入的队伍
     *
     * @param teamMyQuery - 查询我管理的队伍请求封装对象
     * @param loginUser - 当前登录用户
     * @return 符合要求的所有队伍
     */
    @Override
    public List<Team> listMyJoinTeams(TeamMyQuery teamMyQuery, User loginUser) {
        if (teamMyQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        Long userId = teamMyQuery.getId();
        Long loginUserId = loginUser.getId();
        if (userId == null || userId <= 0 || loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        if (!userId.equals(loginUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 去重
        Map<Long, List<UserTeam>> tempResult = list.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> teamIdList = new ArrayList<>(tempResult.keySet());
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.in("id", teamIdList);
        return this.list(teamQueryWrapper);
    }

    /**
     * 根据队伍 id, 统计队伍成员数
     *
     * @param teamId - 队伍 id
     * @return 当前队伍成员数
     */
    private long countTeamUserByTeamId(long teamId) {
        // 队伍人数校验
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId - 队伍 id
     * @return 队伍
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }
}
