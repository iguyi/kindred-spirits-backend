package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.Message;
import com.guyi.kindredspirits.service.MessageService;
import com.guyi.kindredspirits.mapper.MessageMapper;
import org.springframework.stereotype.Service;

/**
 * 针对表 message(消息表) 的数据库操作 Service 实现
* @author 孤诣
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService{

}




