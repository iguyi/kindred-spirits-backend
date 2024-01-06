package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.request.ChatRequest;
import com.guyi.kindredspirits.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
     * 私聊
     *
     * @param chatRequest - 聊天请求
     * @return
     */
    @PostMapping("/private")
    public BaseResponse<Object> privateChat(@RequestBody ChatRequest chatRequest) {
        if (chatRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数未空");
        }
        return ResultUtils.success(new Object());
    }

}
