package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.MessageRequest;
import com.guyi.kindredspirits.model.vo.FriendVo;
import com.guyi.kindredspirits.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
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
     * @param messageRequest - 消息封装类: sender 向 receiver 进行好友申请
     * @return 好友申请的消息存储成功，返回 true; 否则, 返回 false
     */
    Boolean applyFriend(MessageRequest messageRequest);

    /**
     * 同意好友申请
     *
     * @param activeUserId  - activeUser 向 passiveUser 发出好友申请
     * @param passiveUserId - passiveUser 同意 activeUser 的好友申请
     * @param loginUser     - 当前登录用户
     * @return 新数据 id
     */
    Long agreeFriendApply(Long activeUserId, Long passiveUserId, User loginUser);

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
     * @param friendId           - 好友 id
     * @param httpServletRequest - 客户端请求
     * @return 对应好友信息
     */
    FriendVo showFriend(Long friendId, HttpServletRequest httpServletRequest);

}
