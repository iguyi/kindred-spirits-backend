package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagRequest;
import com.guyi.kindredspirits.service.TagService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 标签接口
 *
 * @author 张仕恒
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
    public BaseResponse<Boolean> addSingleTag(@RequestBody TagRequest tagSingle
            , HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (tagSingle == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        boolean result = tagService.addSingleTag(tagSingle, loginUser);
        return new BaseResponse<>(0, result);
    }
}
