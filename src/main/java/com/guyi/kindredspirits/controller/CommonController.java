package com.guyi.kindredspirits.controller;

import cn.hutool.core.util.IdUtil;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;
import com.guyi.kindredspirits.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Optional;

/**
 * 通用
 *
 * @author 张仕恒
 */
@RestController
@RequestMapping("/common")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
@ConfigurationProperties("project")
@Data
public class CommonController {

    /**
     * 用户头像存放位置
     */
    private String userAvatarPath;

    /**
     * 头像对应 url 前缀
     */
    private String urlPrefix;

    @Resource
    private UserService userService;

    @PostMapping("/avatar/user")
    public BaseResponse<Integer> userRegister(@RequestBody MultipartFile avatar,
                                              HttpServletRequest httpServletRequest) {

        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        try {
            // 写文件
            String originalFilename = avatar.getOriginalFilename();
            String fileName = getFileName(originalFilename);
            avatar.transferTo(new File(userAvatarPath, fileName));
            System.out.println(userAvatarPath + fileName);

            // 更新用户信息
            String avatarUrl = urlPrefix + "/user/" + fileName;
            UserUpdateRequest updateUser = new UserUpdateRequest();
            updateUser.setId(loginUser.getId());
            updateUser.setAvatarUrl(avatarUrl);
            int result = userService.updateUser(updateUser, loginUser);
            if (result < 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
            }

            return new BaseResponse<>(0, result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
        }
    }

    /**
     * 根据 originalFilename 重新生成一个唯一的文件名
     *
     * @param originalFilename - 源文件的文件名
     * @return 拓展名和源文件一致的唯一的文件名
     */
    private String getFileName(String originalFilename) {
        String avatarName = Optional.ofNullable(originalFilename).orElse(".png");
        String[] split = avatarName.split("\\.");
        System.out.println(IdUtil.objectId() + "." + split[split.length - 1]);
        return IdUtil.objectId() + "." + split[split.length - 1];
    }

}
