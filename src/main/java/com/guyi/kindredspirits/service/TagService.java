package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;

/**
 * @author 张仕恒
 */
public interface TagService extends IService<Tag> {

    /**
     * 创建一个新的标签, 仅管理员可操作
     *
     * @param tagSingle - 创建标签请求封装类对象
     * @param loginUser - 当前登录用户
     * @return 返回 true 表示创建成功
     */
    boolean addSingleTag(TagAddRequest tagSingle, User loginUser);

}
