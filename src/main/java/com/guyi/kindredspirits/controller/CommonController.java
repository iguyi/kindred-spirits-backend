package com.guyi.kindredspirits.controller;

import cn.hutool.core.util.IdUtil;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ProjectProperties;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
public class CommonController {

    @Resource
    private ProjectProperties projectProperties;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 上传用户头像
     *
     * @param avatar             - 文件数据
     * @param httpServletRequest - 客户端请求
     * @return 1-上传成功
     */
    @PostMapping("/avatar/user")
    public BaseResponse<Integer> userAvatar(@RequestBody MultipartFile avatar, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);

        try {
            // 写文件
            String originalFilename = avatar.getOriginalFilename();
            String fileName = getFileName(originalFilename);
            avatar.transferTo(new File(projectProperties.getUserAvatarPath(), fileName));

            // 更新用户信息
            String avatarUrl = projectProperties.getUrlPrefix() + "/user/" + fileName;
            UserUpdateRequest updateUser = new UserUpdateRequest();
            updateUser.setId(loginUser.getId());
            updateUser.setAvatarUrl(avatarUrl);
            int result = userService.updateUser(updateUser, loginUser, httpServletRequest);
            if (result < 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
            }

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("用户头像上传错误\n", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
        }
    }

    /**
     * 上传队伍头像
     *
     * @param avatar             - 文件数据
     * @param teamId             - 队伍 id
     * @param httpServletRequest - 客户端请求
     * @return 1-上传成功
     */
    @PostMapping("/avatar/team")
    public BaseResponse<Integer> teamAvatar(@RequestBody MultipartFile avatar, Long teamId,
                                            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);

        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, ErrorCode.PARAMS_ERROR.getMsg());
        }


        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, ErrorCode.NULL_ERROR.getMsg());
        }

        if (!loginUser.getId().equals(team.getLeaderId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, ErrorCode.NO_AUTH.getMsg());
        }

        try {
            // 写文件
            String originalFilename = avatar.getOriginalFilename();
            String fileName = getFileName(originalFilename);
            avatar.transferTo(new File(projectProperties.getTeamAvatarPath(), fileName));

            // 更新队伍信息
            Team updateTeam = new Team();
            updateTeam.setId(teamId);
            updateTeam.setAvatarUrl(projectProperties.getUrlPrefix() + "/team/" + fileName);
            boolean updateResult = teamService.updateById(updateTeam);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传错误");
            }

            return ResultUtils.success(1);
        } catch (Exception e) {
            log.error("队伍头像上传错误\n", e);
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
