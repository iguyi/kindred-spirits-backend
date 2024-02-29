package com.guyi.kindredspirits.controller;

import com.google.gson.reflect.TypeToken;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.TagConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;
import com.guyi.kindredspirits.model.vo.TagVo;
import com.guyi.kindredspirits.service.TagService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.redis.RecreationCache;
import com.guyi.kindredspirits.util.redis.RedisQueryReturn;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 标签接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/tag")
@Slf4j
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private UserService userService;

    /**
     * 创建单个标签
     *
     * @param tagSingle          - 单个标签
     * @param httpServletRequest - 客户端请求
     * @return - true 表示添加成功
     */
    @PostMapping("/add/single")
    public BaseResponse<Boolean> addSingleTag(@RequestBody TagAddRequest tagSingle,
                                              HttpServletRequest httpServletRequest) {

        //  参数检验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (tagSingle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }

        // 新增标签
        boolean result = tagService.addSingleTag(tagSingle, loginUser);
        if (result) {
            return ResultUtils.success(true);
        }

        // 标签创建失败
        log.error("标签创建失败");
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签创建失败");
    }

    /**
     * 获取标签的基本信息。<br/>
     * 基本信息包括: id、标签名、对应顶级标签 id、权值。
     *
     * @return 根据顶级标签分组后的标签基本信息
     */
    @GetMapping("/get/all")
    public BaseResponse<List<List<TagVo>>> getAll() {
        // 数据对应的 Redis Key
        String redisKey = String.format(RedisConstant.KEY_PRE, "tag", "get-all", "simple");

        // 数据对应的类型信息
        Type tagVoListType = new TypeToken<List<List<TagVo>>>() {
        }.getType();

        // 从缓存中获取数据
        RedisQueryReturn<List<List<TagVo>>> redisQueryReturn = RedisUtil.getValue(redisKey, tagVoListType);

        // 数据处理
        List<List<TagVo>> groupTagVoList = redisQueryReturn.getData();
        if (groupTagVoList != null) {
            // 数据在缓存中存在
            if (redisQueryReturn.isExpiration()) {
                // 缓存数据过期, 需要重新构建缓存
                RecreationCache.recreation(() -> {
                    boolean recreationResult = RedisUtil.setValue(redisKey, tagService.getAll(),
                            TagConstant.TAG_CACHE_TIMEOUT,
                            TagConstant.UNIT);
                    if (!recreationResult) {
                        log.error("缓存重构失败");
                    }
                });
            }

            return ResultUtils.success(groupTagVoList);
        }

        // 缓存中不存在数据, 从数据查询数据, 并将其写入缓存
        groupTagVoList = tagService.getAll();
        boolean result = RedisUtil.setValue(redisKey, groupTagVoList, TagConstant.TAG_CACHE_TIMEOUT, TagConstant.UNIT);
        if (!result) {
            log.error("缓存设置失败");
        }

        return ResultUtils.success(groupTagVoList);
    }

    /**
     * 分组获取标签的基本信息。<br/>
     * 基本信息包括: id、标签名、对应顶级标签 id、权值。
     *
     * @param httpServletRequest - 客户端请求
     * @return - 分组后的标签列表请求响应封装对象列表
     */
    @GetMapping("/simple/list")
    public BaseResponse<List<List<TagVo>>> getTagGroup(HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(tagService.getTagGroup());
    }

}
