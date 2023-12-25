package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.User;

import java.util.List;

/**
 * 针对表 friend(好友表) 的数据库操作 Service 接口
 *
 * @author 孤诣
 */
public interface FriendService extends IService<Friend> {

    /**
     * 同意好友申请
     *
     * @param activeUserId  - activeUser 向 passiveUser 发出好友申请
     * @param passiveUserId - passiveUser 同意 activeUser 的好友申请
     * @param loginUser     - 当前登录用户
     * @return 新数据 id
     */
    Long agreeFriendRequest(Long activeUserId, Long passiveUserId, User loginUser);

    /**
     * 查询好友列表
     *
     * @param loginUser - 当前登录用户
     * @return 好友列表
     */
    List<User> getFriendList(User loginUser);

}
