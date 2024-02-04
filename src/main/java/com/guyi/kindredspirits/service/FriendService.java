package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.MessageRequest;
import com.guyi.kindredspirits.model.request.ProcessFriendApplyRequest;
import com.guyi.kindredspirits.model.request.UpdateRelationRequest;
import com.guyi.kindredspirits.model.vo.FriendVo;

import java.util.List;

/**
 * 针对表 friend(好友表) 的数据库操作 Service 接口
 *
 * @author 孤诣
 */
public interface FriendService extends IService<Friend> {

    /**
     * 好友申请
     *
     * @param loginUser      - 当前登录用户
     * @param messageRequest - 消息封装类: sender 向 receiver 进行好友申请
     * @return 好友申请的消息存储成功，返回 true; 否则, 返回 false
     */
    Boolean applyFriend(User loginUser, MessageRequest messageRequest);

    /**
     * 处理好友申请
     *
     * @param processFriendApplyRequest - 处理好友申请请求封装
     * @param loginUser                 - 当前登录用户
     * @return 处理结果
     */
    Boolean agreeFriendApply(ProcessFriendApplyRequest processFriendApplyRequest, User loginUser);

    /**
     * 查询好友列表
     *
     * @param loginUser - 当前登录用户
     * @return 好友列表
     */
    List<User> getFriendList(User loginUser);

    /**
     * 查看好友信息
     *
     * @param loginUser - 当前登录用户
     * @param friendId  - 好友 id
     * @return 对应好友信息
     */
    FriendVo showFriend(User loginUser, Long friendId);

    /**
     * 更新和好友的关系
     *
     * @param loginUser             - 当前登录用户
     * @param updateRelationRequest - 更新好友状态请求封装
     * @return 更新结果
     */
    Boolean updateRelation(User loginUser, UpdateRelationRequest updateRelationRequest);

}
