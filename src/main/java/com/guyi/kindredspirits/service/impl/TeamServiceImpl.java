package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.TeamMapper;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.model.enums.TeamStatusEnum;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

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
}

