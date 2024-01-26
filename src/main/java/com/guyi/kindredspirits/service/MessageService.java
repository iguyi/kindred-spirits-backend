package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.vo.MessageVo;

import java.util.List;

/**
 * 针对表 message(消息表) 的数据库操作 Service
 *
 * @author 孤诣
 */
public interface MessageService extends IService<Message> {

    /**
     * 查询当前用户的所有消息
     *
     * @param loginUser 当前登录用户
     * @return 当前用户接收的所有消息的列表
     */
    List<MessageVo> getMessageList(User loginUser);

}
