package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.FriendMapper;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.enums.FriendRelationStatusEnum;
import com.guyi.kindredspirits.service.FriendService;
import com.guyi.kindredspirits.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 针对表 friend(好友表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    @Resource
    private UserService userService;

    /**
     * "A 同意 B 的好友申请" / "B 向 A 发出好友申请, 那么在 user 表中根据 AId 和 BId 查询, 要查询到两条记录
     */
    private static final long TWO_PEOPLE = 2L;

    @Override
    public Long agreeFriendRequest(Long activeUserId, Long passiveUserId, User loginUser) {
        if (activeUserId == null || passiveUserId == null || activeUserId <= 0 || passiveUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        if (passiveUserId.equals(loginUser.getId())) {
            // passiveUser 为被添加人, 当前登录用户必须是被添加人才可以同意和 activeUser 建立好友关系
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }

        // 用户真实性判断
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("id", activeUserId).or().eq("id", passiveUserId);
        long count = userService.count(userQueryWrapper);
        if (count != TWO_PEOPLE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // todo 考虑是否 拉黑 等状况

        // 保存数据
        Friend newFriend = new Friend();
        newFriend.setActiveUserId(activeUserId);
        newFriend.setPassiveUserId(passiveUserId);
        this.save(newFriend);

        return newFriend.getId();
    }

    @Override
    public List<User> getFriendList(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户登录数据出错, 请重新登录");
        }

        // 查询所有好友 id
        QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
        friendQueryWrapper.eq("activeUserId", loginUserId)
                .eq("relationStatus", FriendRelationStatusEnum.NORMAL.getValue())
                .or()
                .eq("passiveUserId", loginUserId);
        List<Friend> friendRecordList = this.list(friendQueryWrapper);
        List<Long> friendIdList = friendRecordList.stream()
                .map(friendRecord -> {
                    Long activeUserId = friendRecord.getActiveUserId();
                    return !loginUserId.equals(activeUserId) ? activeUserId : friendRecord.getPassiveUserId();
                })
                .collect(Collectors.toList());

        // 查询所有好友的详细信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (friendIdList.size() == 0) {
            return null;
        }
        userQueryWrapper.in("id", friendIdList);
        return userService.list(userQueryWrapper);
    }

}
