package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.mapper.ChatMapper;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.model.vo.WebSocketVo;
import com.guyi.kindredspirits.service.ChatService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Resource
    HttpServletRequest httpServletRequest;

    @Override
    public ChatVo getChatVo(User senderUser, User receiverUser, String chatContent, ChatTypeEnum chatTypeEnum) {
        WebSocketVo senderUserLogo = new WebSocketVo();
        BeanUtils.copyProperties(senderUser, senderUserLogo);

        WebSocketVo receiverUserLogo = new WebSocketVo();
        BeanUtils.copyProperties(receiverUser, receiverUserLogo);

        ChatVo chatVo = new ChatVo();
        chatVo.setSenderUser(senderUserLogo);
        chatVo.setReceiverUser(receiverUserLogo);
        chatVo.setChatContent(chatContent);
        chatVo.setChatType(chatTypeEnum.getType());
        DateTime sendTime = DateUtil.date(System.currentTimeMillis());
        chatVo.setSendTime(DateUtil.format(sendTime, "yyyy-MM-dd HH:mm:ss"));

        return chatVo;
    }

}




