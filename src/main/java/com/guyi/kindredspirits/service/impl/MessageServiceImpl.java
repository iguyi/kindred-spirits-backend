package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.mapper.MessageMapper;
import com.guyi.kindredspirits.model.domain.Message;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.vo.MessageVo;
import com.guyi.kindredspirits.service.MessageService;
import com.guyi.kindredspirits.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对表 message(消息表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Resource
    private UserService userService;

    @Override
    public List<MessageVo> getMessageList(User loginUser) {
        // 获取当前用户接收的消息
        Long loginUserId = loginUser.getId();
        QueryWrapper<Message> messageQueryWrapper = new QueryWrapper<>();
        messageQueryWrapper
                .select("id", "senderId", "receiverId", "messageType", "messageBody", "processed", "createTime")
                .eq("receiverId", loginUserId);
        List<Message> messageList = this.list(messageQueryWrapper);

        // 消息发送者 id
        List<Long> senderIdList = new ArrayList<>();
        // 返回结果
        List<MessageVo> result = new ArrayList<>();

        // 处理当前用户接收的消息
        messageList.forEach(message -> {
            MessageVo messageVo = new MessageVo();
            BeanUtils.copyProperties(message, messageVo);

            Long senderId = message.getSenderId();
            if (senderId.equals(0L)) {
                messageVo.setSendName("系统通知");
            } else {
                senderIdList.add(senderId);
            }

            result.add(messageVo);
        });

        if (!senderIdList.isEmpty()) {
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.select("id", "username").in("id", senderIdList);
            List<User> senderUserList = userService.list(userQueryWrapper);
            result.forEach(messageVo -> {
                for (User user : senderUserList) {
                    if (user.getId().equals(messageVo.getSenderId())) {
                        messageVo.setSendName(user.getUsername());
                        break;
                    }
                }
            });
        }

        // 按日期排序, 越新的消息越靠前
        result.sort((t1, t2) -> {
            String time1 = DateUtil.format(t1.getCreateTime(), "yyyyMMddHHmmss");
            String time2 = DateUtil.format(t2.getCreateTime(), "yyyyMMddHHmmss");
            return Math.toIntExact(Long.parseLong(time2) - Long.parseLong(time1));
        });

        return result;
    }
}




