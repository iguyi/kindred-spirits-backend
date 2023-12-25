package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.contant.RedisConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;
import com.guyi.kindredspirits.model.vo.TagVo;
import com.guyi.kindredspirits.service.TagService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.JsonUtil;
import com.guyi.kindredspirits.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/tag")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private UserService userService;

    /**
     * 添加单个标签
     *
     * @param tagSingle - 单个标签
     * @return - true 表示添加成功
     */
    @PostMapping("/add/single")
    public BaseResponse<Boolean> addSingleTag(@RequestBody TagAddRequest tagSingle
            , HttpServletRequest httpServletRequest) {

        //  用户是否登录
        User loginUser = userService.getLoginUser(httpServletRequest);

        boolean result = tagService.addSingleTag(tagSingle, loginUser);
        if (!result) {
            log.error("标签创建失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签创建失败");
        }

        return new BaseResponse<>(0, true);
    }

    /**
     * 获取标签的基本信息。
     * 基本信息包括: id、标签名、对应顶级标签 id、权值。
     *
     * @return 根据顶级标签分组后的标签基本信息
     */
    @GetMapping("/get/all")
    public BaseResponse<List<List<TagVo>>> getAll() {
        String redisKey = String.format(RedisConstant.KEY_PRE, "tag", "get-all", "simple");
        List<List<TagVo>> groupTagVoList = new ArrayList<>();
        groupTagVoList = RedisUtil.get(redisKey, groupTagVoList.getClass());
        if (groupTagVoList != null) {
            return new BaseResponse<>(0, groupTagVoList);
        }

        groupTagVoList = tagService.getAll();
        // 写缓存
        boolean result = RedisUtil.setForValue(redisKey, groupTagVoList);
        if (!result) {
            log.error("Set cache error in '/tag/get/all'.");
        }
        return new BaseResponse<>(0, groupTagVoList);
    }

}
