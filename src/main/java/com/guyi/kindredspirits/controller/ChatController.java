package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatHistoryRequest;
import com.guyi.kindredspirits.model.vo.ChatRoomVo;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 聊天接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    /**
     * 获取私聊室的历史聊天记录
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     * @param httpServletRequest - 客户端请求
     * @return 私聊室的历史聊天记录列表
     */
    @PostMapping("/private")
    public BaseResponse<List<ChatVo>> getPrivateChat(@RequestBody ChatHistoryRequest chatHistoryRequest,
                                                     HttpServletRequest httpServletRequest) {
        // 参数校验
        if (chatHistoryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);

        return ResultUtils.success(chatService.getPrivateChat(loginUser, chatHistoryRequest));
    }

    /**
     * 获取队伍聊天室的历史聊天记录
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     * @param httpServletRequest - 客户端请求
     * @return 队伍聊天室的历史聊天记录列表
     */
    @PostMapping("/team")
    public BaseResponse<List<ChatVo>> getTeamChat(@RequestBody ChatHistoryRequest chatHistoryRequest,
                                                  HttpServletRequest httpServletRequest) {
        // 参数校验
        if (chatHistoryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);

        return ResultUtils.success(chatService.getTeamChat(loginUser, chatHistoryRequest));
    }

    /**
     * 获取历史聊天会话列表
     *
     * @param httpServletRequest - 客户端请求
     * @return 历史聊天会话列表
     */
    @GetMapping("/room/list")
    public BaseResponse<List<ChatRoomVo>> getChatRoomList(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.getChatRoomList(loginUser));
    }

}
