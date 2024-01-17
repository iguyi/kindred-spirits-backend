package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.FriendMapper;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.Message;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.enums.FriendRelationStatusEnum;
import com.guyi.kindredspirits.model.enums.MessageTypeEnum;
import com.guyi.kindredspirits.model.request.MessageRequest;
import com.guyi.kindredspirits.service.FriendService;
import com.guyi.kindredspirits.service.MessageService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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

    @Resource
    private FriendMapper friendMapper;

    @Resource
    private MessageService messageService;

    @Resource
    private HttpServletRequest httpServletRequest;

    /**
     * "好友申请" 和 "同意好友申请" 时, 根据相关用户的 id 应在数据库中查询到的数据的数量:
     * - 消息接收者同意消息发送者的好友申请
     * - 消息发送者向消息接收者发出好友申请
     * 那么在 user 表中, 根据二者 id 作为查询条件, 要查询到两条记录
     */
    private static final long TWO_PEOPLE = 2L;

    @Override
    public Boolean applyFriend(MessageRequest messageRequest) {
        // 校验发送者和接收者的 id 是否正确
        if (messageRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        Long senderId = messageRequest.getSenderId();
        Long receiverId = messageRequest.getReceiverId();
        if (senderId == null || receiverId == null || senderId.equals(receiverId) || senderId * receiverId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        // A 是否为当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        if (!loginUser.getId().equals(senderId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }

        Integer messageType = messageRequest.getMessageType();
        if (!MessageTypeEnum.VERIFY_MESSAGE.getType().equals(messageType)) {
            // 类型必须是验证消息
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 校验是否存在 B 用户
        User receiverUser = userService.getById(receiverId);
        if (receiverUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方不存在");
        }

        // 合法性校验
        Long count = friendMapper.count();
        if (count != 0) {
            /*
             可能的情况:
             - 被对方拉黑了
             - 将对方拉黑了
             - 互相拉黑
             - 已经是好友了
             */
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止操作");
        }

        // 存储申请信息，待 B 进行处理, 并将结果返回
        Message message = new Message();
        BeanUtils.copyProperties(messageRequest, message);
        message.setId(null);
        message.setProcessed(0);
        boolean res = messageService.save(message);
        if (res) {
            // 消息保存至 Redis
            String key = String.format(RedisConstant.MESSAGE_VERIFY_KEY_PRE, message.getId() + "_" + receiverId);
            RedisUtil.setForValue(key, message);
            return true;
        }
        return false;
    }

    @Override
    public Long agreeFriendApply(Long activeUserId, Long passiveUserId, User loginUser) {
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
