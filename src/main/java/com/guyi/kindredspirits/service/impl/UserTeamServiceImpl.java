package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.mapper.UserTeamMapper;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.service.UserTeamService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 针对表 user_team(用户-队伍关系表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam> implements UserTeamService {

    @Resource
    private UserTeamMapper userTeamMapper;

    @Override
    public List<UserTeam> getMessageByUserId(Long userId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        return this.list(userTeamQueryWrapper);
    }

    @Override
    public List<UserTeam> getMessageByTeamId(Long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return this.list(userTeamQueryWrapper);
    }

    @Override
    public Boolean correlation(Long userId, Long teamId) {
        if (userId == null || teamId == null || userId * teamId < 1) {
            return false;
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId).eq("teamId", teamId);
        UserTeam userTeam = userTeamMapper.selectOne(userTeamQueryWrapper);
        return userTeam != null;
    }

}




