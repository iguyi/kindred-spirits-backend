package com.guyi.kindredspirits.mapper;

import com.guyi.kindredspirits.model.domain.Friend;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 针对表 friend(好友表) 的数据库操作 Mapper
 *
 * @author 孤诣
 */
public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * todo 传一个 List 作为参数, 根据枚举状态, 多态改变查询条件
     * 查询符合 "activeUser" 和 "passiveUser" 有一放将对方拉黑
     * 或者
     * "activeUser" 和 "passiveUser" 是正常好友的数据记录
     * <p>
     * 主要用于 "申请好友" 的合法性判断, A 向 B 申请好友
     * - B 不能将 A 拉黑
     * - A 也不能将 B 拉黑
     * - A 和 B 当前不能是好友关系(重复添加)
     *
     * @return 符合条件的数据的数量
     */
    Long count();

}




