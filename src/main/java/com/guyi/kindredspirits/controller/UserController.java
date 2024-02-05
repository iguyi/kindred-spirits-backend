package com.guyi.kindredspirits.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.reflect.TypeToken;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UpdatePwdRequest;
import com.guyi.kindredspirits.model.request.UserLoginRequest;
import com.guyi.kindredspirits.model.request.UserRegisterRequest;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;
import com.guyi.kindredspirits.model.vo.UserVo;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.redis.RedisQueryReturn;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 注册用户
     *
     * @param userRegisterRequest - 用户注册请求封装类对象
     * @return 新用户 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest,
                                           HttpServletRequest httpServletRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User newUser = userService.userRegister(userPassword, checkPassword);
        User safetyUser = userService.getSafetyUser(newUser);
        httpServletRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);
        return ResultUtils.success(newUser.getId());
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest   - 用户登录请求封装类对象
     * @param httpServletRequest - httpServletRequest
     * @return 脱敏后的登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, httpServletRequest);
        return ResultUtils.success(user);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public void userLogout(HttpSession httpSession) {
        httpSession.invalidate();
    }

    /**
     * 获取用户登录态
     *
     * @param httpServletRequest - httpServletRequest
     * @return 用户登录态
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrent(HttpServletRequest httpServletRequest) {
        Object objectUser = httpServletRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) objectUser;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long id = currentUser.getId();
        // TODO 校验是否合法
        User user = userService.getById(id);
        User safetyUser = userService.getSafetyUser(user);
        safetyUser.setTags(userService.getTagListJson(safetyUser));
        return ResultUtils.success(safetyUser);
    }

    /**
     * 自由搜索用户
     *
     * @param searchCondition - 搜索条件(关键词)
     * @return 符合要求的用户
     */
    @GetMapping("/search")
    public BaseResponse<List<UserVo>> searchUser(String searchCondition, HttpServletRequest httpServletRequest) {
        // 用户是否登录
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<User> userList = userService.searchUser(searchCondition);
        List<UserVo> result = userList.stream()
                .filter(user -> !user.getId().equals(loginUser.getId()))
                .map(user -> {
                    user.setTags(userService.getTagListJson(user));
                    UserVo userVo = new UserVo();
                    BeanUtils.copyProperties(user, userVo);
                    return userVo;
                }).collect(Collectors.toList());
        return ResultUtils.success(result);
    }


    /**
     * 查询所有用户, 仅管理员可用
     * todo 考虑分页查询
     *
     * @param httpServletRequest - httpServletRequest
     * @return 所有用户
     */
    @GetMapping("/search/all")
    public BaseResponse<List<User>> searchUserAll(HttpServletRequest httpServletRequest) {
        // 当前登录用户是否是管理员
        if (!userService.isAdmin(httpServletRequest)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 查询并脱敏
        List<User> userList = userService.list(new QueryWrapper<>());
        userList.forEach(user -> {
            String tagListJson = userService.getTagListJson(user);
            user.setTags(tagListJson);
        });

        List<User> users = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(users);
    }

    /**
     * 根据用户 id 查询用户信息
     *
     * @param id - 用户 id
     * @return 对应 id 的用户的响应信息
     */
    @GetMapping("/search/id")
    public BaseResponse<UserVo> getUserById(Long id, HttpServletRequest httpServletRequest) {
        // 用户是否登录
        userService.getLoginUser(httpServletRequest);
        User user = userService.getById(id);
        user.setTags(userService.getTagListJson(user));
        UserVo res = new UserVo();
        BeanUtils.copyProperties(user, res);
        return ResultUtils.success(res);
    }

    /**
     * 根据 username 查询用户
     * todo 考虑分页查询
     *
     * @param username - 用户昵称
     * @return 符合要求的用户列表
     */
    @GetMapping("/search/username")
    public BaseResponse<List<User>> searchUsersByUsername(String username, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        // 用户昵称校验
        if (StringUtils.isAnyBlank(username) || username.length() > UserConstant.USERNAME_MAX) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 查询并脱敏
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("username", username);
        List<User> userList = userService.list(queryWrapper);
        List<User> users = userList.stream().map(user -> {
            user = userService.getSafetyUser(user);
            String tagListJson = userService.getTagListJson(user);
            user.setTags(tagListJson);
            return user;
        }).collect(Collectors.toList());

        return ResultUtils.success(users);
    }

    /**
     * 根据标签查询用户
     *
     * @param tagNameList - 用户标签列表
     * @return 符合要求的用户
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 推荐相似用户
     * todo 暂时是随机返回, 缓存也需要分页查询
     * todo 查询缓存后, 要判断数量对不对啊！
     * todo 排除已经是好友的用户
     * todo 排除自己
     *
     * @param pageSize           - 每页的数据量, >0
     * @param pageNum            - 页码, >0
     * @param httpServletRequest - httpServletRequest
     * @return 和当前用户相似的用户
     */
    @GetMapping("/recommend")
    public BaseResponse<List<User>> recommends(long pageSize, long pageNum, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (pageSize < 1 || pageNum < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 查询缓存
        final String redisKey = String.format(RedisConstant.RECOMMEND_KEY_PRE, loginUser.getId());
        Type userListType = new TypeToken<List<User>>() {
        }.getType();
        RedisQueryReturn<List<User>> redisQueryReturn = RedisUtil.getValue(redisKey, userListType);
        List<User> userList = redisQueryReturn.getData();
        if (userList != null) {
            // 数据存在缓存, 直接返回缓存中的数据
            return ResultUtils.success(userList);
        }

        // 缓存中不存在数据, 从数据查询数据, 并将其写入缓存
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        userList = userPage.getRecords();
        userList = userList.stream().map(user -> {
            user.setTags(userService.getTagListJson(user));
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());

        // 15 小时 + 随机时间
        long timeout = RedisConstant.PRECACHE_TIMEOUT + RandomUtil.randomLong(15 * 60L);
        boolean result = RedisUtil.setValue(redisKey, userList, timeout, TimeUnit.MINUTES);
        if (!result) {
            log.error("缓存设置失败");
        }
        return ResultUtils.success(userList);
    }

    /**
     * 获取最匹配的用户
     * todo 排除自己
     * todo 排除已经是好友的用户
     *
     * @param num - 推荐的数量
     * @return 和当前登录用户最匹配的 num 个其他用户
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest httpServletRequest) {
        if (num <= 0 || num > UserConstant.MATCH_NUM) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest - 更新用户请求参数
     * @return 更改的数据总量, 正常应该是 1
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest httpServletRequest) {
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "无可修改的信息");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        Integer result = userService.updateUser(userUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户更新密码
     *
     * @param updatePwdRequest - 请求封装类
     * @return 更新密码的结果
     */
    @PostMapping("/update/pwd")
    public BaseResponse<Boolean> updatePwd(@RequestBody UpdatePwdRequest updatePwdRequest, HttpSession httpSession,
                                           HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (updatePwdRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Boolean result = userService.updatePwd(loginUser, updatePwdRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码修改失败");
        }
        // 密码修改成功后, 用户需要重新登录
        this.userLogout(httpSession);
        return ResultUtils.success(true);
    }

    /**
     * 管理员根据 id 删除用户(逻辑删除)
     *
     * @param id 用户 id
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest httpServletRequest) {
        // 仅管理员可删除
        if (!userService.isAdmin(httpServletRequest)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

}
