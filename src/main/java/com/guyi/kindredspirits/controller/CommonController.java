package com.guyi.kindredspirits.controller;

import cn.hutool.core.util.IdUtil;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;
import com.guyi.kindredspirits.service.TeamService;
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
 * @author 孤诣
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
     * 队伍头像存放位置
     */
    private String teamAvatarPath;

    /**
     * 头像对应 url 前缀
     */
    private String urlPrefix;

    @Resource
    private HttpServletRequest httpServletRequest;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 上传用户头像
     *
     * @param avatar - 文件数据
     * @return 1-上传成功
     */
    @PostMapping("/avatar/user")
    public BaseResponse<Integer> userAvatar(@RequestBody MultipartFile avatar) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        try {
            // 写文件
            String originalFilename = avatar.getOriginalFilename();
            String fileName = getFileName(originalFilename);
            avatar.transferTo(new File(userAvatarPath, fileName));

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
     * 上传队伍头像
     *
     * @param avatar - 文件数据
     * @param teamId - 队伍 id
     * @return 1-上传成功
     */
    @PostMapping("/avatar/team")
    public BaseResponse<Integer> teamAvatar(@RequestBody MultipartFile avatar, @RequestBody Long teamId) {
        User loginUser = userService.getLoginUser();

        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }


        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, ErrorCode.NULL_ERROR.getMsg());
        }

        if (loginUser.getId().equals(team.getLeaderId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, ErrorCode.NO_AUTH.getMsg());
        }

        try {
            // 写文件
            String originalFilename = avatar.getOriginalFilename();
            String fileName = getFileName(originalFilename);
            avatar.transferTo(new File(teamAvatarPath, fileName));

            // 更新队伍信息
            Team updateTeam = new Team();
            updateTeam.setId(teamId);
            updateTeam.setAvatarUrl(urlPrefix + "/team/" + fileName);
            boolean updateResult = teamService.updateById(updateTeam);

            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
            }

            return new BaseResponse<>(0, 1);
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
        return IdUtil.objectId() + "." + split[split.length - 1];
    }

}
