package com.guyi.kindredspirits.controller;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.TagAddRequest;
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

}
