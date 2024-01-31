package com.guyi.kindredspirits.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.model.domain.Message;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.enums.MessageTypeEnum;
import com.guyi.kindredspirits.model.vo.MessageVo;
import com.guyi.kindredspirits.service.MessageService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    /**
     * 查询当前用户的所有消息
     *
     * @return 当前用户接收的所有消息的列表
     */
    @GetMapping("/list")
    public BaseResponse<List<MessageVo>> getMessageList(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(messageService.getMessageList(loginUser));
    }

    /**
     * 统计未读消息数量
     *
     * @param httpServletRequest - 客户端请求
     * @return 未读消息数量
     */
    @GetMapping("/undressed")
    public BaseResponse<Long> countUndressed(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        QueryWrapper<Message> messageQueryWrapper = new QueryWrapper<>();
        messageQueryWrapper
                .eq("receiverId", loginUser.getId())
                .and(queryWrapper -> queryWrapper
                        .eq("processed", 0)
                        .or().isNull("processed"));
        long count = messageService.count(messageQueryWrapper);
        return ResultUtils.success(count);
    }

    /**
     * 刷新当前用户未读的系统消息状态 - 未读的系统消息更改为已读
     *
     * @param httpServletRequest 客户端请求
     * @return 刷新数据的数量
     */
    @GetMapping("/refresh")
    public BaseResponse<Boolean> refresh(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        UpdateWrapper<Message> messageUpdateWrapper = new UpdateWrapper<>();
        messageUpdateWrapper
                .set("processed", 1)
                .eq("receiverId", loginUser.getId())
                .eq("messageType", MessageTypeEnum.SYSTEM_MESSAGE.getType());
        boolean result = messageService.update(messageUpdateWrapper);
        return ResultUtils.success(result);
    }

}
