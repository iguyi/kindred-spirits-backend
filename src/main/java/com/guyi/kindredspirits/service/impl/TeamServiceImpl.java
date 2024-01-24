package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.TeamMapper;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.model.enums.TeamStatusEnum;
import com.guyi.kindredspirits.model.request.*;
import com.guyi.kindredspirits.model.vo.TeamAllVo;
import com.guyi.kindredspirits.model.vo.UserSimpleVo;
import com.guyi.kindredspirits.model.vo.UserTeamVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import com.guyi.kindredspirits.util.EntityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 针对表 team(队伍表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

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
        if (maxNum < 2 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求!");
        }
        //  队伍标题长度验证
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 10) {
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
        Long teamId = team.getId();
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
        return 1;
    }

    @Override
    public List<UserTeamVo> listTeams(TeamQueryRequest teamQuery, boolean isAdmin) {
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
                // 默认查询公开的队伍
                statusEnum = TeamStatusEnum.PUBLIC;
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
        // 判断新数据和旧数据是否相等
        if (EntityUtil.entityEq(teamUpdateRequest, oldTeam)) {
            return true;
        }
        Integer newTeamStatus = teamUpdateRequest.getStatus();
        TeamStatusEnum newEnumTeamStatus = TeamStatusEnum.getEnumByValue(newTeamStatus);
        if (TeamStatusEnum.SECRET.equals(newEnumTeamStatus)) {
            // 修改为加密队伍
            Integer oldTeamStatus = oldTeam.getStatus();
            TeamStatusEnum oldEnumTeamStatus = TeamStatusEnum.getEnumByValue(oldTeamStatus);
            String oldPassword = oldTeam.getPassword();
            if (!newEnumTeamStatus.equals(oldEnumTeamStatus)) {
                // 原队伍不是加密的
                if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须要有密码");
                }
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 获取锁对象: 不同用户对有不同的 lock key, 保证不会阻塞其他用户对的请求
        final String lockKey = "kindredspirits:teamservice:jointeam:" + loginUserId;
        RLock lock = redissonClient.getLock(lockKey);
        int counter = 0;

        // 限制重试次数, 防死锁
        while (counter < 100) {
            counter++;
            try {
                if (lock.tryLock(0, 30L, TimeUnit.SECONDS)) {
                    // 用户所属队伍数量校验
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", loginUserId);
                    List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                    if (userTeamList.size() >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属队伍不能操作 5 个");
                    }
                    for (UserTeam userTeam : userTeamList) {
                        if (userTeam.getTeamId().equals(teamId)) {
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
                    Integer num = team.getNum();
                    if (num >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 更新当前队伍人数
                    team.setNum(num + 1);
                    boolean updateResult = this.updateById(team);

                    // 新增用户-队伍关联信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUserId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean saveResult = userTeamService.save(userTeam);

                    if (updateResult && saveResult) {
                        return true;
                    }

                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
                }
            } catch (InterruptedException e) {
                log.debug("The doCacheRecommendUser method of the PreCacheJob class is error: " + e);
            } finally {
                // 只释放当前线程加的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitOrDeleteRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long teamId = teamQuitRequest.getTeamId();
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 获取队伍信息
        Team team = this.getTeamById(teamId);

        // 获取用户信息
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(loginUserId);

        // 判断是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }

        // 队伍人数校验
        Integer num = team.getNum();
        if (num == 1) {
            // 队伍只有 1 人, 删除队伍相关信息
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
            if (loginUserId.equals(team.getLeaderId())) {
                // 队长不能退出
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队长不能退出队伍");
            }

            // 退出队伍
            userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("userId", loginUserId);
            userTeamQueryWrapper.eq("teamId", teamId);
            boolean removeResult = userTeamService.remove(userTeamQueryWrapper);

            // 更新队伍人数
            team.setNum(num - 1);
            boolean updateResult = this.updateById(team);
            if (removeResult && updateResult) {
                return true;
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
        }
    }

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

    @Override
    public Page<Team> listTeamsByPage(Long loginUserId, TeamQueryRequest teamQuery) {
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        // 查询当前用户已加入的队伍
        List<UserTeam> messageByUserId = userTeamService.getMessageByUserId(loginUserId);
        List<Long> currentUserInTeamIds = messageByUserId.stream()
                .map(UserTeam::getTeamId)
                .collect(Collectors.toList());

        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        if (currentUserInTeamIds.size() > 0) {
            // 过滤 "加入的队伍"
            teamQueryWrapper.notIn("id", currentUserInTeamIds);
        }

        // 过滤 "过期的队伍"
        teamQueryWrapper.ge("expireTime", new Date());
        Page<Team> teamPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        return this.page(teamPage, teamQueryWrapper);
    }

    @Override
    public List<Team> listMyLeaderTeams(TeamMyQueryRequest teamMyQuery, User loginUser) {
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

    @Override
    public List<Team> listMyJoinTeams(TeamMyQueryRequest teamMyQuery, User loginUser) {
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

    @Override
    public List<Team> searchTeam(String searchCondition) {
        userService.getLoginUser();
        if (StringUtils.isBlank(searchCondition)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, ErrorCode.NULL_ERROR.getMsg());
        }

        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper
                .and(queryWrapper -> queryWrapper
                        .eq("id", searchCondition)
                        .or().like("name", searchCondition)
                        .or().like("description", searchCondition)
                )
                .and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        return this.list(teamQueryWrapper);
    }

    @Override
    public TeamAllVo checkTeam(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }
        User loginUser = userService.getLoginUser();
        Long loginUserId = loginUser.getId();

        // 查询该目标队伍的 "用户-队伍" 关系
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.select("userId").eq("teamId", teamId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if (userTeamList.isEmpty()) {
            throw new BusinessException(ErrorCode.NULL_ERROR, ErrorCode.NULL_ERROR.getMsg());
        }

        // 获取目标队伍所有成员的 id
        List<Long> inTeamUserIdList = userTeamList.stream().map(UserTeam::getUserId).collect(Collectors.toList());
        if (!inTeamUserIdList.contains(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, ErrorCode.NO_AUTH.getMsg());
        }

        // 整合返回结果
        TeamAllVo result = new TeamAllVo();

        // 获取队伍信息
        Team team = this.getById(teamId);
        BeanUtils.copyProperties(team, result);

        // 获取队伍所有成员信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "username", "avatarUrl").in("id", inTeamUserIdList);
        List<User> userList = userService.list(userQueryWrapper);
        // 转化为用于返回的类型
        List<UserSimpleVo> userSimpleVoList = userList.stream().map(user -> {
            UserSimpleVo userSimpleVo = new UserSimpleVo();
            BeanUtils.copyProperties(user, userSimpleVo);
            return userSimpleVo;
        }).collect(Collectors.toList());
        result.setUserList(userSimpleVoList);

        return result;
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean kickOut(OperationMemberRequest operationMemberRequest) {
        // 校验
        Team team = operationParamCheck(operationMemberRequest);
        Long teamId = team.getId();
        Long memberId = operationMemberRequest.getMemberId();

        // 踢出
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId).eq("userId", memberId);
        boolean removeResult = userTeamService.remove(userTeamQueryWrapper);
        if (!removeResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMsg());
        }

        // 更新队伍人数
        team.setNum(team.getNum() - 1);
        boolean updateResult = this.updateById(team);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMsg());
        }

        return true;
    }

    @Override
    public Boolean abdicator(OperationMemberRequest operationMemberRequest) {
        // 校验
        Team team = operationParamCheck(operationMemberRequest);
        Long memberId = operationMemberRequest.getMemberId();

        // 位置转让
        team.setLeaderId(memberId);
        return this.updateById(team);
    }

    @Override
    public String refreshLink(Long teamId) {
        if (teamId == null || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        // 刷新入队链接者必须是队长
        User loginUser = userService.getLoginUser();
        Long loginUserId = loginUser.getId();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.select("id", "teamLink").eq("id", teamId).eq("leaderId", loginUserId);
        Team team = teamMapper.selectOne(teamQueryWrapper);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        // 生成入队邀请码
        String newTeamLink = IdUtil.simpleUUID();
        team.setTeamLink(newTeamLink);
        boolean result = this.updateById(team);
        if (result) {
            return newTeamLink;
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
    }

    /**
     * 对队长操作队员时的参数、权限进行统一校验
     *
     * @param operationMemberRequest - 队长操作队伍成员请求封装类对象
     * @return 队长、被操作队员的所在队伍
     */
    private Team operationParamCheck(OperationMemberRequest operationMemberRequest) {
        // 参数校验
        if (operationMemberRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }
        Long memberId = operationMemberRequest.getMemberId();
        Long teamId = operationMemberRequest.getTeamId();
        if (memberId == null || teamId == null || memberId < 1 || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        // 用户登录校验
        User loginUser = userService.getLoginUser();

        // 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        // 权限校验
        Long loginUserId = loginUser.getId();
        if (!team.getLeaderId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, ErrorCode.NO_AUTH.getMsg());
        }

        // 成员是否在队伍中
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId).eq("userId", memberId);
        long result = userTeamService.count(userTeamQueryWrapper);
        if (result != 1) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return team;
    }

}
