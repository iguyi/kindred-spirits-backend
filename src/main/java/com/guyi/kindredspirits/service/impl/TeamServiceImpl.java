package com.guyi.kindredspirits.service.impl;

import java.util.Date;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ProjectProperties;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.TeamConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.ChatMapper;
import com.guyi.kindredspirits.mapper.TeamMapper;
import com.guyi.kindredspirits.model.domain.*;
import com.guyi.kindredspirits.model.enums.TeamStatusEnum;
import com.guyi.kindredspirits.model.request.*;
import com.guyi.kindredspirits.model.vo.TeamAllVo;
import com.guyi.kindredspirits.model.vo.UserSimpleVo;
import com.guyi.kindredspirits.model.vo.UserTeamVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import com.guyi.kindredspirits.util.EntityUtil;
import com.guyi.kindredspirits.util.lock.LockUtil;
import com.guyi.kindredspirits.ws.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 不使用 ChatService 是为了避免循环依赖
     */
    @Resource
    private ChatMapper chatMapper;

    @Resource
    private UnreadMessageNumService unreadMessageNumService;

    private static final String SESSION_NAME_TEMPLATE = "team-%s-%s";

    @Resource
    private ProjectProperties projectProperties;

    /**
     * Team 的 Lock Key 模板, 最后一个占位符将来填入用户 id
     */
    private static final String TEAM_LOCK_KEY =
            String.format(RedisConstant.LOCK_KEY, "team-service", "create-join-team", "%s");

    @Override
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
        if (maxNum < TeamConstant.MINX_NUMBER_PEOPLE || maxNum > TeamConstant.MAX_NUMBER_PEOPLE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求!");
        }

        //  队伍标题长度验证
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > TeamConstant.MAX_TEAM_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不符合要求!");
        }

        //  队伍描述验证
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > TeamConstant.MAX_DESCRIPTION_LENGTH) {
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
            if (StringUtils.isBlank(password) || password.length() > TeamConstant.MAX_PASSWORD_LENGTH) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置错误!");
            }
        }

        //  超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间设置错误!");
        }

        // 同步
        String lockKey = String.format(TEAM_LOCK_KEY, userId);
        final int waitTime = 0;
        final long leaseTime = 30L;
        boolean result = LockUtil.opsRedissonLockRetries(lockKey, waitTime, leaseTime, TimeUnit.SECONDS, redissonClient,
                () -> {
                    //  校验用户以【加入/创建】的队伍不超过 5 个
                    QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
                    teamQueryWrapper.eq("userId", userId);
                    long hasTeamNum = userTeamService.count(teamQueryWrapper);
                    if (hasTeamNum >= TeamConstant.MAX_HAS_TEAM_NUM) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属队伍不能大于 5 个!");
                    }

                    // 创建队伍
                    return createTeam(team, userId);
                });

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍创建失败!");
        }
        return 1;
    }

    /**
     * 创建队伍
     *
     * @param team   - 被创建队伍的信息
     * @param userId - 创建人 id
     * @return 是否创建成功
     */
    @Transactional(rollbackFor = Exception.class)
    protected boolean createTeam(Team team, Long userId) {
        //  插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        team.setLeaderId(userId);
        team.setAvatarUrl(projectProperties.getDefaultTeamAvatarPath());

        // 生成入队邀请码
        String newTeamLink = IdUtil.simpleUUID();
        team.setTeamLink(newTeamLink);
        boolean saveResult = this.save(team);
        Long teamId = team.getId();
        if (!saveResult || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍创建失败!");
        }

        //  插入数据到 用户-队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        saveResult = userTeamService.save(userTeam);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍创建失败!");
        }

        // 创建会话未读消息记录
        saveResult = createUnreadMessageLog(userId, String.format(SESSION_NAME_TEMPLATE, userId, teamId));
        return saveResult;
    }

    @Override
    public List<UserTeamVo> listTeams(TeamQueryRequest teamQuery, boolean isAdmin) {
        if (teamQuery == null) {
            return Collections.emptyList();
        }

        // 组合查询条件
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
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
        if (maxNum != null && maxNum > 0 && maxNum <= TeamConstant.MAX_NUMBER_PEOPLE) {
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
        // 默认查询公开的队伍
        if (statusEnum == null) {
            statusEnum = TeamStatusEnum.PUBLIC;
        }
        //  非公开队伍需要管理员权限才能查询
        if (!isAdmin && !TeamStatusEnum.PUBLIC.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "查询失败");
        }
        teamQueryWrapper.eq("status", statusEnum.getValue());
        // 不展示已过期队伍
        teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        // 分页查询相关队伍
        Page<Team> teamPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        List<Team> teamList = this.page(teamPage, teamQueryWrapper).getRecords();
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        // 构建返回内容
        List<UserTeamVo> userTeamVoList = new ArrayList<>();
        for (Team team : teamList) {
            // 脱敏
            UserTeamVo userTeamVo = new UserTeamVo();
            BeanUtils.copyProperties(team, userTeamVo);

            // 队伍创始人 id
            Long createTeamUserId = team.getUserId();
            if (createTeamUserId == null) {
                continue;
            }
            // 查询队伍创始人信息
            User user = userService.getById(createTeamUserId);
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
        final String lockKey = String.format(TEAM_LOCK_KEY, loginUserId);
        Boolean result = LockUtil.opsRedissonLockRetries(lockKey, 0, 30L, TimeUnit.SECONDS, redissonClient,
                () -> createUserTeamRelational(teamJoinRequest, loginUserId, teamId));

        if (result == null || !result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
        return true;
    }

    /**
     * 创建 用户-队伍 关系
     *
     * @param teamJoinRequest - 对用户加入队伍的请求消息的封装
     * @param loginUserId     - 登录用户 id
     * @param teamId          - 队伍 id
     * @return true - 加入成功; false - 加入失败
     */
    private boolean createUserTeamRelational(TeamJoinRequest teamJoinRequest, Long loginUserId, Long teamId) {
        // 用户所属队伍数量校验
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", loginUserId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if (userTeamList.size() >= TeamConstant.MAX_HAS_TEAM_NUM) {
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
        boolean saveUserTeamResult = userTeamService.save(userTeam);

        // 创建会话未读消息记录
        String sessionName = String.format(SESSION_NAME_TEMPLATE, loginUserId, teamId);
        boolean saveUnreadResult = createUnreadMessageLog(loginUserId, sessionName);

        if (updateResult && saveUserTeamResult && saveUnreadResult) {
            return true;
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 创建/加入队伍时创建对应的会话未读消息记录
     *
     * @param userId      - 用户 id
     * @param sessionName - 会话名称
     * @return 记录保存是否成功
     */
    private boolean createUnreadMessageLog(Long userId, String sessionName) {
        // 创建会话未读消息记录
        UnreadMessageNum unreadMessageNum = new UnreadMessageNum();
        unreadMessageNum.setUserId(userId);
        unreadMessageNum.setChatSessionName(sessionName);
        unreadMessageNum.setUnreadNum(0);
        return unreadMessageNumService.save(unreadMessageNum);
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
            // 队伍只有 1 人, 解散队伍
            return clearTeamByTeamId(teamId);
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
                WebSocket.removeTeamMemberChatConnect(teamId.toString(), loginUserId.toString());
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
        if (!loginUserId.equals(team.getLeaderId())) {
            // 不是队长, 无权限
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长可以解散队伍");
        }
        // 清理队伍信息
        return clearTeamByTeamId(teamId);
    }

    /**
     * 根据队伍 id 清理和该队伍相关的信息
     *
     * @param teamId - 队伍 id
     * @return true: 成功清理
     */
    private boolean clearTeamByTeamId(Long teamId) {
        // 移除队伍
        boolean removeTeamResult = this.removeById(teamId);
        if (!removeTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
        }
        // 移除 "用户-队伍" 关系
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean removeUserTeamResult = userTeamService.remove(userTeamQueryWrapper);
        if (!removeUserTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
        }
        // 移除聊天记录
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.eq("teamId", teamId);
        int count = chatMapper.countByTeamId(teamId);
        int deleteNum = chatMapper.delete(chatQueryWrapper);
        if (count != deleteNum) {
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

        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
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
        Long leaderId = teamMyQueryRequestParamsValid(teamMyQuery, loginUser);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("leaderId", leaderId);
        return this.list(teamQueryWrapper);
    }

    @Override
    public List<Team> listMyJoinTeams(TeamMyQueryRequest teamMyQuery, User loginUser) {
        Long userId = teamMyQueryRequestParamsValid(teamMyQuery, loginUser);
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
     * 判断 查询我加入/我管理的队伍请求 的参数是否正确: <br/>
     * - 如果正确, 返回当前登录用户的 id. <br/>
     * - 如果错误, 抛异常. <br/>
     *
     * @param teamMyQuery - 查询我加入/我管理队伍请求封装
     * @param loginUser   - 当前登录用户
     * @return 当前登录用户的 id
     */
    private Long teamMyQueryRequestParamsValid(TeamMyQueryRequest teamMyQuery, User loginUser) {
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
        return userId;
    }

    @Override
    public List<Team> searchTeam(String searchCondition, long pageSize, long pageNum) {
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
        return this.page(new Page<>(pageNum, pageSize), teamQueryWrapper).getRecords();
    }

    @Override
    public TeamAllVo checkTeam(User loginUser, Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

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
    public Boolean kickOut(User loginUser, OperationMemberRequest operationMemberRequest) {
        // 校验
        Team team = operationParamCheck(loginUser, operationMemberRequest);
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
    public Boolean abdicator(User loginUser, OperationMemberRequest operationMemberRequest) {
        // 校验
        Team team = operationParamCheck(loginUser, operationMemberRequest);
        Long memberId = operationMemberRequest.getMemberId();

        // 位置转让
        team.setLeaderId(memberId);
        return this.updateById(team);
    }

    @Override
    public String refreshLink(User loginUser, Long teamId) {
        if (teamId == null || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        // 刷新入队链接者必须是队长
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean joinTeamByLink(TeamJoinRequest teamJoinRequest, User loginUser) {
        // 参数校验
        if (teamJoinRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        String teamLink = teamJoinRequest.getTeamLink();
        Long loginUserId = loginUser.getId();
        if (teamId == null || StringUtils.isBlank(teamLink) || loginUserId == null || teamId * loginUserId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 判断是否已加入过该队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", loginUserId).eq("teamId", teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能重复加入");
        }

        // 查询目标队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper
                .select("id", "maxNum", "num", "expireTime")
                .eq("id", teamId)
                .eq("teamLink", teamLink);
        Team targetTeam = teamMapper.selectOne(teamQueryWrapper);

        if (targetTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "邀请码错误");
        }

        // 判断目标队伍是否以及满员
        Integer maxNum = targetTeam.getMaxNum();
        Integer num = targetTeam.getNum();
        if (maxNum <= num) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "队伍已满");
        }

        // 判断现在是否不在可入队时间(即队伍过期)
        Date now = new Date();
        Date expireTime = targetTeam.getExpireTime();
        if (now.after(expireTime)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "队伍已过期");
        }

        // 创建 "用户-队伍关系"
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUserId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(now);
        boolean saveUserTeamResult = userTeamService.save(userTeam);
        if (!saveUserTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }

        // 更新队伍人数
        targetTeam.setNum(++num);
        boolean updateTeamResult = this.updateById(targetTeam);
        if (!updateTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }

        return true;
    }

    /**
     * 对队长操作队员时的参数、权限进行统一校验
     *
     * @param operationMemberRequest - 队长操作队伍成员请求封装类对象
     * @return 队长、被操作队员的所在队伍
     */
    private Team operationParamCheck(User loginUser, OperationMemberRequest operationMemberRequest) {
        // 参数校验
        if (operationMemberRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }
        Long memberId = operationMemberRequest.getMemberId();
        Long teamId = operationMemberRequest.getTeamId();
        if (memberId == null || teamId == null || memberId < 1 || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

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
