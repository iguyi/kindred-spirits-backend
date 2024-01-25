package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.MessageRequest;
import com.guyi.kindredspirits.model.vo.FriendVo;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.FriendService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 孤诣
 */
@RestController
@RequestMapping("/friend")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class FriendController {

    @Resource
    private FriendService friendService;

    @Resource
    private UserService userService;

    /**
     * 好友申请
     *
     * @param messageRequest - 消息封装类: sender 向 receiver 进行好友申请
     * @return 好友申请的消息存储成功，返回 true; 否则, 返回 false
     */
    @PostMapping("/apply")
    public BaseResponse<Boolean> applyFriend(@RequestBody MessageRequest messageRequest) {
        if (messageRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        Boolean result = friendService.applyFriend(messageRequest);
        if (result) {
            return ResultUtils.success(true);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "消息发送失败");
    }

    /**
     * 同意好友申请
     *
     * @param activeUserId  - activeUser 向 passiveUser 发出好友申请
     * @param passiveUserId - passiveUser 同意 activeUser 的好友申请
     * @return > 0 表示关系创建成功
     */
    @PostMapping("/agree")
    public BaseResponse<Long> agreeFriendApply(Long activeUserId, Long passiveUserId) {
        User loginUser = userService.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        Long res = friendService.agreeFriendApply(activeUserId, passiveUserId, loginUser);
        if (res == null || res <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统异常");
        }
        return new BaseResponse<>(0, res);
    }

    /**
     * 查询好友列表
     *
     * @return 好友列表
     */
    @GetMapping("/list")
    public BaseResponse<List<UserVo>> getFriendList() {
        User loginUser = userService.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        List<User> friendList = friendService.getFriendList(loginUser);
        if (friendList == null || friendList.size() == 0) {
            return new BaseResponse<>(0, null);
        }

        List<UserVo> finalFriendList = new ArrayList<>();
        friendList.forEach(friend -> {
            friend.setTags(userService.getTagListJson(friend));
            UserVo finalFriend = new UserVo();
            BeanUtils.copyProperties(friend, finalFriend);
            finalFriendList.add(finalFriend);
        });

        return new BaseResponse<>(0, finalFriendList);
    }

    /**
     * 查看好友信息
     *
     * @param friendId           - 好友 id
     * @param httpServletRequest - 客户端请求
     * @return 对应好友信息
     */
    @GetMapping("/show")
    public BaseResponse<FriendVo> showFriend(Long friendId, HttpServletRequest httpServletRequest) {
        if (friendId == null || friendId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }
        return ResultUtils.success(friendService.showFriend(friendId, httpServletRequest));
    }

}
