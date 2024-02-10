package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatSessionStateRequest;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 未读消息/聊天记录统计表
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/unread")
@Slf4j
public class UnreadMessageNumController {

    @Resource
    private UserService userService;

    @Resource
    private UnreadMessageNumService unreadMessageNumService;

    /**
     * 设置聊天会话状态
     *
     * @param stateRequest       - 设置聊天会话状态请求参数的封装
     * @param httpServletRequest - 客户端请求
     */
    @PostMapping("/setting/session/state")
    public void setSessionState(@RequestBody ChatSessionStateRequest stateRequest,
                                HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (stateRequest == null) {
            log.error("stateRequest is null");
            return;
        }

        unreadMessageNumService.setSessionSate(loginUser, stateRequest);
    }

}
