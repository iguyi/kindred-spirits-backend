package com.guyi.kindredspirits.mapper;

import com.guyi.kindredspirits.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 针对表 user(用户表) 的数据库操作 Mapper
 *
 * @author 孤诣
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 批量插入用户数据
     *
     * @param userList - 用户列表
     * @return 操作结果
     */
    boolean batchInsert(List<User> userList);

}




