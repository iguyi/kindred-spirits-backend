package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.Friend;
import com.guyi.kindredspirits.service.FriendService;
import com.guyi.kindredspirits.mapper.FriendMapper;
import org.springframework.stereotype.Service;

/**
 * 针对表 friend(好友表) 的数据库操作 Service 实现
 *
 * @author 张仕恒
 */
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend>
        implements FriendService {

}
