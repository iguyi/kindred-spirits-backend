package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.TagMapper;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagRequest;
import com.guyi.kindredspirits.service.TagService;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author 张仕恒
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {

    @Resource
    private TagMapper tagMapper;

    /**
     * 添加单个标签, 需要管理员权限
     *
     * @param tagSingle - 单个标签
     * @param user      - 执行此操作的用户
     * @return 返回 true 表示标签添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSingleTag(TagRequest tagSingle, User user) {
        // 检验是否能否执行此操作
        if (!generalExamine(tagSingle, user, UserConstant.ADMIN_ROLE)) {
            return false;
        }

        TagService proxy = (TagService) AopContext.currentProxy();
        Integer level = tagSingle.getLevel();
        if (level.equals(0)) {
            proxy.addOneLevel(tagSingle, user);
        } else if (level.equals(1)) {
            proxy.addTwoLevel(tagSingle, user);
        } else {
            proxy.addThreeLevel(tagSingle, user);
        }
        return true;
    }

    /**
     * 添加一级标签
     *
     * @param tagSingle - 一级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOneLevel(TagRequest tagSingle, User user) {
        // 检验是否能否执行此操作
        if (!generalExamine(tagSingle, user, UserConstant.ADMIN_ROLE)) {
            return false;
        }

        Integer level = tagSingle.getLevel();
        // 不是一级标签
        if (!level.equals(0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        Double weights = tagSingle.getWeights();
        Long parentId = tagSingle.getParentId();

        //  对于一级标签, 必须指定权值且不能有父标签
        if (weights == null || parentId != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        //  数据写入数据库
        TagService proxy = (TagService) AopContext.currentProxy();
        Tag newTag = proxy.saveByTagRequest(tagSingle);
        Long newTagId = newTag.getId();

        // 给一级标签添加一个默认二级标签
        Tag childTag = new Tag();
        childTag.setTagName(tagSingle.getTagName() + "-其他");
        childTag.setUserId(user.getId());
        childTag.setParentId(newTagId);
        childTag.setCount(0);
        childTag.setLevel(1);
        childTag.setWeights(weights / 20);
        boolean result = this.save(childTag);
        if (!result || childTag.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签添加失败");
        }

        return true;
    }

    /**
     * 添加二级标签
     *
     * @param tagSingle - 二级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTwoLevel(TagRequest tagSingle, User user) {
        // 检验是否能否执行此操作
        if (!generalExamine(tagSingle, user, UserConstant.ADMIN_ROLE)) {
            return false;
        }

        Tag parentTag = addTwoLeveOrThreeLeve(tagSingle);

        // 父标签必须是一级标签、父标签的 count 不能 >= 20
        Integer parentTagLevel = parentTag.getLevel();
        Integer parentTagCount = parentTag.getCount();
        if (parentTagLevel.equals(0) || parentTagCount >= 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        //  数据写入数据库
        TagService proxy = (TagService) AopContext.currentProxy();
        proxy.saveByTagRequest(tagSingle);

        // 更新父标签的 count
        parentTag.setCount(++parentTagCount);
        int updateNum = tagMapper.updateById(parentTag);
        if (updateNum == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签添加失败");
        }

        return true;
    }

    /**
     * 添加三级标签
     *
     * @param tagSingle - 三级标签
     * @param user      - 执行此操作的用户
     * @return true 添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addThreeLevel(TagRequest tagSingle, User user) {
        // 检验是否能否执行此操作
        if (!generalExamine(tagSingle, user, UserConstant.ADMIN_ROLE)) {
            return false;
        }

        Tag parentTag = addTwoLeveOrThreeLeve(tagSingle);

        // 父标签必须是二级标签
        Integer parentTagLevel = parentTag.getLevel();
        if (parentTagLevel.equals(1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        //  数据写入数据库
        TagService proxy = (TagService) AopContext.currentProxy();
        proxy.saveByTagRequest(tagSingle);

        // 更新父标签的 count
        Integer parentTagCount = parentTag.getCount();
        parentTag.setCount(++parentTagCount);
        int updateNum = tagMapper.updateById(parentTag);
        if (updateNum == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签添加失败");
        }

        return true;
    }

    /**
     * 将 TagRequest 转为对应的 Tag 对象, 在保存到数据库中
     *
     * @param tagSingle - 标签
     * @return 添加标签对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag saveByTagRequest(TagRequest tagSingle) {
        Tag newTag = new Tag();
        BeanUtils.copyProperties(tagSingle, newTag);
        boolean result = this.save(newTag);
        Long newTagId = newTag.getId();
        if (!result || newTagId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签添加失败");
        }
        return newTag;
    }

    /**
     * 检查当前用户是否有权限操作 tagSingle
     *
     * @param tagSingle - 标签
     * @param user      - 执行此操作的用户
     * @param userRole  - 用户需要的权限
     * @return 返回 true 表示通过检查
     */
    @Override
    public boolean generalExamine(TagRequest tagSingle, User user, int userRole) {
        // 必须指定用户
        if (user == null) {
            return false;
        }

        //  需要管理员权限
        if (userRole == UserConstant.ADMIN_ROLE) {
            Integer currentUserRole = user.getUserRole();
            Long userId = user.getId();
            if (userId == null || userId < 1 || currentUserRole == null || currentUserRole != UserConstant.ADMIN_ROLE) {
                return false;
            }
            tagSingle.setUserId(userId);
        }

        //  参数是否为空
        if (tagSingle == null) {
            return false;
        }

        //  数据校验: 【标签名】和【标签层级】必须指定
        Integer level = tagSingle.getLevel();
        String tagName = tagSingle.getTagName();
        if (tagName == null || level == null || level < 0 || level > 2) {
            return false;
        }

        Long parentId = tagSingle.getParentId();
        Double weights = tagSingle.getWeights();

        //  对于【一级标签】: 必须指定权值且不能有父标签
        return !level.equals(0) || weights != null || parentId == null;
    }

    /**
     * 对 "判断二级和三级标签是否符合添加条件" 的封装
     *
     * @param tagSingle - 标签
     * @return TagRequest 对应的 Tag 对象
     */
    @Override
    public Tag addTwoLeveOrThreeLeve(TagRequest tagSingle) {
        // 二级标签必须指定父标签
        Long parentId = tagSingle.getParentId();
        if (parentId == null || parentId < 0L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 二级标签的父标签必须存
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parentId", parentId);
        Tag parentTag = tagMapper.selectOne(queryWrapper);
        if (parentTag == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        return parentTag;
    }
}
