package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.FriendMapper;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.Message;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.enums.FriendRelationStatusEnum;
import com.guyi.kindredspirits.common.enums.MessageTypeEnum;
import com.guyi.kindredspirits.model.enums.UpdateFriendRelationOperationEnum;
import com.guyi.kindredspirits.model.request.MessageRequest;
import com.guyi.kindredspirits.model.request.ProcessFriendApplyRequest;
import com.guyi.kindredspirits.model.request.UpdateRelationRequest;
import com.guyi.kindredspirits.model.vo.FriendVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.FriendService;
import com.guyi.kindredspirits.service.MessageService;
import com.guyi.kindredspirits.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private MessageService messageService;

    @Resource
    private FriendMapper friendMapper;

    /**
     * sender 向 receiverId 进行好友申请
     */
    @Override
    public Boolean applyFriend(User loginUser, MessageRequest messageRequest) {
        // 校验接收者的 id 是否正确
        if (messageRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        Long receiverId = messageRequest.getReceiverId();
        if (receiverId == null || receiverId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        Integer messageType = messageRequest.getMessageType();
        if (!MessageTypeEnum.VERIFY_MESSAGE.getType().equals(messageType)) {
            // 类型必须是验证消息
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 校验消息接收者是否存在
        User receiverUser = userService.getById(receiverId);
        if (receiverUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方不存在");
        }

        // 合法性校验
        QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
        Long loginUserId = loginUser.getId();
        friendQueryWrapper
                .and(queryWrapper -> queryWrapper
                        .eq("activeUserId", loginUserId)
                        .eq("passiveUserId", receiverId)
                        .or(wrapper -> wrapper
                                .eq("activeUserId", receiverId)
                                .eq("passiveUserId", loginUserId)
                        )
                )
                .in("relationStatus", 0, 3, 4, 5, 6);
        long count = this.count(friendQueryWrapper);
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

        // 存储申请信息，待消息接收者进行处理, 并将结果返回
        Message message = new Message();
        BeanUtils.copyProperties(messageRequest, message);
        message.setId(null);
        message.setSenderId(loginUserId);
        message.setMessageBody("好友申请");
        message.setProcessed(0);
        // todo 设置缓存（ hash 结构）
        return messageService.save(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean agreeFriendApply(ProcessFriendApplyRequest processFriendApplyRequest, User loginUser) {
        // 参数校验
        if (processFriendApplyRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long senderId = processFriendApplyRequest.getSenderId();
        Boolean isAgreed = processFriendApplyRequest.getIsAgreed();
        Long loginUserId = loginUser.getId();
        if (senderId == null || senderId < 1 || isAgreed == null || loginUserId == null || loginUserId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将消息设置为已处理
        UpdateWrapper<Message> messageUpdateWrapper = new UpdateWrapper<>();
        messageUpdateWrapper.set("processed", 1).eq("senderId", senderId).eq("receiverId", loginUserId);
        boolean updateMessageResult = messageService.update(messageUpdateWrapper);
        if (!updateMessageResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }

        // 构建系统消息, 告知好友申请者结果
        Message message = new Message();
        message.setSenderId(0L);
        message.setReceiverId(senderId);
        message.setMessageType(MessageTypeEnum.SYSTEM_MESSAGE.getType());

        String username = loginUser.getUsername();
        if (isAgreed) {
            // 好友验证通过
            // 验证二者曾经是否是好友关系
            QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
            friendQueryWrapper
                    .select("id", "relationStatus")
                    .and(queryWrapper -> queryWrapper.eq("activeUserId", loginUserId)
                            .eq("passiveUserId", senderId)
                            .or(wrapper -> wrapper
                                    .eq("activeUserId", senderId)
                                    .eq("passiveUserId", loginUserId)
                            )
                    )
                    .in("relationStatus", FriendRelationStatusEnum.ACTIVE_DELETE.getValue(),
                            FriendRelationStatusEnum.PASSIVE_DELETE.getValue(),
                            FriendRelationStatusEnum.ALL_DELETE.getValue()
                    );
            Friend friend = friendMapper.selectOne(friendQueryWrapper);
            if (friend != null) {
                // 曾经是好友, 更新关系
                friend.setActiveUserId(senderId);
                friend.setPassiveUserId(loginUserId);
                friend.setRelationStatus(0);
                this.updateById(friend);
            } else {
                // 曾经不是好友
                friend = new Friend();
                friend.setActiveUserId(senderId);
                friend.setPassiveUserId(loginUserId);
                // 保存好友关系
                boolean saveFriendResult = this.save(friend);
                if (!saveFriendResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
                }
            }
            message.setMessageBody(username + "通过了您的好友申请");
        } else {
            // 好友验证未通过
            message.setMessageBody(username + "拒绝了您的好友申请");
        }

        // 保存消息
        boolean saveMessageResult = messageService.save(message);
        if (!saveMessageResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }

        return true;
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

    @Override
    public FriendVo showFriend(User loginUser, Long friendId) {
        if (friendId == null || friendId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        Long loginUserId = loginUser.getId();
        QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
        friendQueryWrapper
                .eq("passiveUserId", loginUserId)
                .eq("activeUserId", friendId)
                .or(queryWrapper -> queryWrapper
                        .eq("passiveUserId", friendId)
                        .eq("activeUserId", loginUserId)
                );
        Friend friend = friendMapper.selectOne(friendQueryWrapper);
        if (friend == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        User friendUser = userService.getById(friendId);
        friendUser.setTags(userService.getTagListJson(friendUser));
        UserVo friendUserVo = new UserVo();
        BeanUtils.copyProperties(friendUser, friendUserVo);

        FriendVo friendVo = new FriendVo();
        BeanUtils.copyProperties(friend, friendVo);
        friendVo.setFriend(friendUserVo);
        friendVo.setIsActive(loginUserId.equals(friend.getActiveUserId()));

        return friendVo;
    }

    @Override
    public Boolean updateRelation(User loginUser, UpdateRelationRequest updateRelationRequest) {
        if (updateRelationRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }
        Long friendId = updateRelationRequest.getFriendId();
        Integer operation = updateRelationRequest.getOperation();
        Boolean isActive = updateRelationRequest.getIsActive();
        UpdateFriendRelationOperationEnum operationType = UpdateFriendRelationOperationEnum.getEnumByValue(operation);
        if (friendId == null || friendId < 1 || operationType == null || isActive == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        Long loginUserId = loginUser.getId();

        QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
        friendQueryWrapper.select("id", "relationStatus");
        UpdateWrapper<Friend> friendUpdateWrapper = new UpdateWrapper<>();
        if (isActive) {
            return isActiveOperation(friendId, operationType, loginUserId, friendQueryWrapper, friendUpdateWrapper);
        } else {
            return notActiveOperation(friendId, operationType, loginUserId, friendQueryWrapper, friendUpdateWrapper);
        }
    }

    /**
     * 在当前用户不是主动添加对方的情况下, 对对方进行删除或拉黑操作
     *
     * @param friendId            - 好友 id
     * @param operationType       - 操作类型
     * @param loginUserId         - 当前登录用户 id
     * @param friendQueryWrapper  - 查询构造器
     * @param friendUpdateWrapper - 更新构造器
     * @return 操作是否成功
     */
    private Boolean notActiveOperation(Long friendId,
                                       UpdateFriendRelationOperationEnum operationType,
                                       Long loginUserId,
                                       QueryWrapper<Friend> friendQueryWrapper,
                                       UpdateWrapper<Friend> friendUpdateWrapper) {
        // 之前不是当前用户主动添加的对方
        friendQueryWrapper.eq("activeUserId", friendId).eq("passiveUserId", loginUserId);
        friendUpdateWrapper.eq("activeUserId", friendId).eq("passiveUserId", loginUserId);

        if (UpdateFriendRelationOperationEnum.DELETE.equals(operationType)) {
            // 之前是对方加的当前用户好友, 现在当前用户将对方删除
            friendQueryWrapper
                    .ne("relationStatus", FriendRelationStatusEnum.PASSIVE_DELETE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_DELETE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ACTIVE_HATE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            Friend friend = friendMapper.selectOne(friendQueryWrapper);
            if (friend == null) {
                return true;
            }

            if (friend.getRelationStatus().equals(FriendRelationStatusEnum.PASSIVE_DELETE.getValue())) {
                // 对方已经将当前用户删除
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ALL_DELETE.getValue());
            } else {
                // 对方没有将当前用户删除
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.PASSIVE_DELETE.getValue());
            }
        } else {
            // 之前是对方加的当前用户好友, 现在当前用户将对方拉黑
            friendQueryWrapper
                    .ne("relationStatus", FriendRelationStatusEnum.PASSIVE_HATE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            Friend friend = friendMapper.selectOne(friendQueryWrapper);
            if (friend == null) {
                return true;
            }

            if (friend.getRelationStatus().equals(FriendRelationStatusEnum.ACTIVE_HATE.getValue())) {
                // 对方已经将当前用户拉黑
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            } else {
                // 对方没有将当前用户拉黑
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.PASSIVE_HATE.getValue());
            }
        }
        return this.update(friendUpdateWrapper);
    }

    /**
     * 在当前用户是主动添加对方的情况下, 对对方进行删除或拉黑操作
     *
     * @param friendId            - 好友 id
     * @param operationType       - 操作类型
     * @param loginUserId         - 当前登录用户 id
     * @param friendQueryWrapper  - 查询构造器
     * @param friendUpdateWrapper - 更新构造器
     * @return 操作是否成功
     */
    private boolean isActiveOperation(Long friendId,
                                      UpdateFriendRelationOperationEnum operationType,
                                      Long loginUserId,
                                      QueryWrapper<Friend> friendQueryWrapper,
                                      UpdateWrapper<Friend> friendUpdateWrapper) {
        // 之前是当前用户主动添加的对方
        friendQueryWrapper.eq("activeUserId", loginUserId).eq("passiveUserId", friendId);
        friendUpdateWrapper.eq("activeUserId", loginUserId).eq("passiveUserId", friendId);

        if (UpdateFriendRelationOperationEnum.DELETE.equals(operationType)) {
            // 当前用户主动添加对方后, 又将对方对方删除了
            friendQueryWrapper
                    .ne("relationStatus", FriendRelationStatusEnum.ACTIVE_DELETE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_DELETE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ACTIVE_HATE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            Friend friend = friendMapper.selectOne(friendQueryWrapper);
            if (friend == null) {
                return true;
            }

            if (friend.getRelationStatus().equals(FriendRelationStatusEnum.PASSIVE_DELETE.getValue())) {
                // 对方已经将当前用户删除
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ALL_DELETE.getValue());
            } else {
                // 对方没有将当前用户删除
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ACTIVE_DELETE.getValue());
            }
        } else {
            // 当前用户主动添加对方后, 然后将对方对方拉黑了
            friendQueryWrapper
                    .ne("relationStatus", FriendRelationStatusEnum.ACTIVE_HATE.getValue())
                    .ne("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            Friend friend = friendMapper.selectOne(friendQueryWrapper);
            if (friend == null) {
                return true;
            }

            if (friend.getRelationStatus().equals(FriendRelationStatusEnum.PASSIVE_HATE.getValue())) {
                // 对方已经将当前用户拉黑
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ALL_HATE.getValue());
            } else {
                // 对方没有将当前用户拉黑
                friendUpdateWrapper.set("relationStatus", FriendRelationStatusEnum.ACTIVE_HATE.getValue());
            }
        }
        return this.update(friendUpdateWrapper);
    }

}
