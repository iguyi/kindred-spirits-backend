package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.request.ChatHistoryRequest;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 聊天接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 获取历史聊天记录列表
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     * @return 历史聊天记录列表
     */
    @PostMapping("/private")
    public BaseResponse<List<ChatVo>> getPrivateChat(@RequestBody ChatHistoryRequest chatHistoryRequest) {
        if (chatHistoryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        return ResultUtils.success(chatService.getPrivateChat(chatHistoryRequest));
    }

}
