package com.guyi.kindredspirits.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserLoginRequest;
import com.guyi.kindredspirits.model.request.UserRegisterRequest;
import com.guyi.kindredspirits.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author 张仕恒
 */
@RestController
@RequestMapping("/user")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis key 的模板字符串
     */
    public static final String REDIS_KEY_PRE = "kindredspirits:user:%s:%s";

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

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
     * 登录态
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
        return ResultUtils.success(safetyUser);
    }

    /**
     * 查询所有用户
     *
     * @param username
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest httpServletRequest) {
        // 仅管理员可查询
        if (!userService.isAdmin(httpServletRequest)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isAnyBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> users = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(users);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 推荐相似用户
     * todo 推荐多个, 为实现
     *
     * @param pageSize:          每页的数据量, >0
     * @param pageNum:           页码, >0
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommends(long pageSize, long pageNum, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (pageSize < 1 || pageNum < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 如果缓存有数据, 直接读缓存
        final String redisKey = String.format(REDIS_KEY_PRE, "recommend", loginUser.getId());
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        // 无缓存, 走数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 写缓存
        try {
            // todo 缓存击穿、缓存雪崩的问题
            // 120 分钟
            valueOperations.set(redisKey, userPage, 120, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error: ", e);
        }
        return ResultUtils.success(userPage);
    }

    /**
     * 获取最匹配的用户
     *
     * @param num - 推荐的数量
     * @return 和当前登录用户最匹配的 num 个其他用户
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest httpServletRequest) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }

    /**
     * todo User 需要封装 dto
     * 更新用户信息
     *
     * @param user - 用户的新信息
     * @return 更改的数据总量, 正常应该是 1
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest httpServletRequest) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无可修改的信息");
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        Integer result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
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
