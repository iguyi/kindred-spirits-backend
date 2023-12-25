package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.TagMapper;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;
import com.guyi.kindredspirits.model.vo.TagVo;
import com.guyi.kindredspirits.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 孤诣
 */
@Service
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Override
    public boolean addSingleTag(TagAddRequest tagSingle, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        //  用户 id 是否正确
        Long loginUserId = loginUser.getId();
        if (loginUserId == null || loginUserId < 1) {
            throw new BusinessException(ErrorCode.NO_AUTH, "用户参数错误");
        }
        //  必须是管理员
        Integer loginUserRole = loginUser.getUserRole();
        if (loginUserRole == null || !loginUserRole.equals(UserConstant.ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        //  是否传递被创建标签信息
        if (tagSingle == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "参数为空");
        }
        //  被创建标签信息校验
        String tagName = tagSingle.getTagName();
        Integer baseWeight = tagSingle.getBaseWeight();
        if (StringUtils.isBlank(tagName) || baseWeight == null || baseWeight < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //  补充被创建标签的创建者信息
        tagSingle.setUserId(loginUserId);

        //  计算被创建标签的最终权值
        Long parentId = tagSingle.getParentId();
        Tag newTag = new Tag();
        BeanUtils.copyProperties(tagSingle, newTag);
        if (parentId != null) {
            //  如果指定了父标签，验证父标签是否存在
            Tag parentTag;
            QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
            tagQueryWrapper.eq("id", parentId);
            parentTag = tagMapper.selectOne(tagQueryWrapper);
            if (parentTag == null) {
                log.error("父标签不存在");
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            // 最终权值 = 父标签最终权值 + 自己的基本权值
            newTag.setWeights(parentTag.getWeights() + newTag.getBaseWeight());
        } else {
            // 最终权值 = 父标签最终权值 + 自己的基本权值
            newTag.setWeights(newTag.getBaseWeight());
        }

        //  将数据写入数据库
        return this.save(newTag);
    }

    @Override
    public List<List<TagVo>> getAll() {
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
        tagQueryWrapper.isNotNull("tagName")
                .isNotNull("userId")
                .isNotNull("isPatent")
                .isNotNull("weights")
                .last("and (isPatent=0 and parentId is not null) or (isPatent=1 and parentId is null)");

        List<Tag> tagAll = tagMapper.selectList(tagQueryWrapper);
        Map<Long, List<Tag>> tagGroup = tagAll.stream()
                .collect(Collectors.groupingBy(tag -> (tag.getIsPatent().equals(1)) ? tag.getId() : tag.getParentId()));
        List<List<TagVo>> resultList = new ArrayList<>();
        tagGroup.forEach((key, value) -> {
            List<TagVo> tempList = new ArrayList<>();
            value.forEach(tag -> {
                TagVo tagVo = new TagVo();
                BeanUtils.copyProperties(tag, tagVo);
                tempList.add(tagVo);
            });
            resultList.add(tempList);
        });

        return resultList;
    }
}
