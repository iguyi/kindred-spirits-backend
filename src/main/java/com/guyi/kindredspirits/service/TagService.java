package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 张仕恒
 */
public interface TagService extends IService<Tag> {

    /**
     * 添加单个标签, 需要管理员权限
     *
     * @param tagSingle - 单个标签
     * @param user      - 执行此操作的用户
     * @return 返回 true 表示标签添加成功
     */
    boolean addSingleTag(TagRequest tagSingle, User user);

    /**
     * 添加一级标签
     *
     * @param tagSingle - 一级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    boolean addOneLevel(TagRequest tagSingle, User user);

    /**
     * 添加二级标签
     *
     * @param tagSingle - 二级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    boolean addTwoLevel(TagRequest tagSingle, User user);

    /**
     * 添加三级标签
     *
     * @param tagSingle - 三级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    boolean addThreeLevel(TagRequest tagSingle, User user);

    /**
     * 将 TagRequest 转为对应的 Tag 对象, 在保存到数据库中
     *
     * @param tagSingle - 标签
     * @return 添加标签对象
     */
    Tag saveByTagRequest(TagRequest tagSingle);

    /**
     * 检查当前用户是否有权限操作 tagSingle
     *
     * @param tagSingle - 标签
     * @param user      - 执行此操作的用户
     * @param userRole  - 用户需要的权限
     * @return 返回 true 表示通过检查
     */
    boolean generalExamine(TagRequest tagSingle, User user, int userRole);

    /**
     * 对 "判断二级和三级标签是否符合添加条件" 的封装
     *
     * @param tagSingle - 标签
     * @return TagRequest 对应的 Tag 对象
     */
    Tag addTwoLeveOrThreeLeve(TagRequest tagSingle);
}
