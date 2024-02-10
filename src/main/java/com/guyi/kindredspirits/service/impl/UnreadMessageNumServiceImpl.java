package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.UnreadMessageNum;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.mapper.UnreadMessageNumMapper;
import org.springframework.stereotype.Service;

/**
 * 针对表 unread_message_num(未读聊天记录统计表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class UnreadMessageNumServiceImpl extends ServiceImpl<UnreadMessageNumMapper, UnreadMessageNum>
        implements UnreadMessageNumService {

}




