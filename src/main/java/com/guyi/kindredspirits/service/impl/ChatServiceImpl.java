package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.mapper.ChatMapper;
import org.springframework.stereotype.Service;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

}




