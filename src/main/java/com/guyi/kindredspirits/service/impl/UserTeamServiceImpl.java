package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.service.UserTeamService;
import com.guyi.kindredspirits.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 孤诣
* @description 针对表【user_team(用户-队伍关系表)】的数据库操作Service实现
* @createDate 2023-10-01 21:57:09
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam> implements UserTeamService{

    @Override
    public List<UserTeam> getMessageByUserId(Long userId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        return this.list(userTeamQueryWrapper);
    }

}




