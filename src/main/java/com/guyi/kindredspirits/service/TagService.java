package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;
import com.guyi.kindredspirits.model.vo.TagVo;

import java.util.List;

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

    /**
     * 获取顶层标签和底层标签的基本信息。
     * 基本信息包括: id、标签名、对应顶级标签 id、权值。
     *
     * @return 根据顶级标签分组后的标签基本信息
     */
    List<List<TagVo>> getAll();
}
