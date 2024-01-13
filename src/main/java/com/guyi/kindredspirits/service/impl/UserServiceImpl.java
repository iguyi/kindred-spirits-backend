package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.contant.BaseConstant;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 孤诣
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private HttpServletRequest httpServletRequest;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "guyi";

    @Override
    public User userRegister(String userPassword, String checkPassword) {
        // 密码校验: 空校验、长度校验、两次输入是否相等、特殊字符匹配
        if (StringUtils.isAnyBlank(userPassword)
                || userPassword.length() < UserConstant.USER_PASSWORD_MIN
                || !userPassword.equals(checkPassword)
                || Pattern.compile(BaseConstant.VALID_PATTER).matcher(userPassword).find()
        ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法密码");
        }

        // 创建新用户
        User newUser = new User();
        // 为新用户生成随机昵称
        String username = UserConstant.DEFAULT_USERNAME_PRE + RandomUtil.randomString(6);
        newUser.setUsername(username);

        // 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        newUser.setUserPassword(encryptPassword);

        String lockKey = String.format(RedisConstant.KEY_PRE, "user", "register", "lock");
        RLock lock = redissonClient.getLock(lockKey);
        for (int i = 0; i < 100; i++) {
            try {
                if (lock.tryLock(0, 30L, TimeUnit.SECONDS)) {
                    // 从缓存中取账号信息最新用户的 userAccount
                    String maxUserAccount = RedisUtil.get(RedisConstant.MAX_ID_USER_ACCOUNT_KEY, String.class);

                    // 如果缓存中没有数据查询数据库中 id 最大的 user 的 userAccount
                    if (maxUserAccount == null) {
                        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                        queryWrapper.orderByDesc("id");
                        User user = page(new Page<>(1, 1), queryWrapper).getRecords().get(0);
                        //  如果缓存和数据库都没有取到 userAccount，那么新用户的 userAccount 为 "100001"
                        maxUserAccount = user == null ? "100001" : user.getUserAccount();
                    }

                    //  将取到的 userAccount 加 1 作为新用户的 userAccount
                    long newUserAccount = Long.parseLong(maxUserAccount) + 1;
                    String newUserAccountValue = String.valueOf(newUserAccount);
                    newUser.setUserAccount(newUserAccountValue);

                    //  新用户数据插入数据库
                    this.save(newUser);
                    if (newUser.getId() == null) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
                    }

                    //  将新用户的 userAccount 写入缓存
                    RedisUtil.setForValue(RedisConstant.MAX_ID_USER_ACCOUNT_KEY, newUserAccountValue);
                    return newUser;
                }
            } catch (Exception e) {
                log.debug("The userRegister method of the PreCacheJob class is error: " + e);
            } finally {
                // 只释放当前线程加的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest) {
        // 非空验证
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名和密码不能为空");
        }

        // 用户名长度验证
        if (userAccount.length() < UserConstant.USER_ACCOUNT_MIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号小于4位");
        }

        // 密码长度验证
        if (userPassword.length() < UserConstant.USER_PASSWORD_MIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
        }

        // 账户不能包含特殊字符
        Matcher matcher = Pattern.compile(BaseConstant.VALID_PATTER).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能含有特殊字符");
        }

        // 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword.");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不存在");
        }

        // 脱敏
        User safetyUser = getSafetyUser(user);

        // 记录用户登录态
        httpServletRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 根据标签搜索用户 -- 内存查询
     *
     * @param tagNameList: 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // 查询所有用户
        List<User> userList = userMapper.selectList(userQueryWrapper);

        // 在内存中查询
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }
            String tagListJson = this.getTagListJson(user);
            user.setTags(tagListJson);
            Set<String> tagSet = JsonUtil.tagsToSet(tagListJson);
            tagSet = Optional.ofNullable(tagSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (tagSet.contains(tagName)) {
                    return true;
                }
            }
            return false;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签搜索用户 -- SQL 查询
     *
     * @param tagNameList: 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    @Override
    public List<User> searchUsersByTagsBySql(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有条件");
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // like '%Java%' and like '%Python%'
        for (String tagName : tagNameList) {
            userQueryWrapper = userQueryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(userQueryWrapper);
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public User getLoginUser() {
        return getLoginUser(this.httpServletRequest);
    }

    /**
     * 从 Session 中获取当前登录用户信息, 并判断是否登录
     *
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return null;
        }
        Object userObj = httpServletRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        return (User) userObj;
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest - 用户的新信息
     * @param loginUser         - 当前登录用户
     * @return 更改的数据总量
     */
    @Override
    public int updateUser(UserUpdateRequest userUpdateRequest, User loginUser) {
        Long userId = userUpdateRequest.getId();
        if (userId == null || userId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 不是管理员且修改的不是自己的信息
        if (!isAdmin(loginUser) && !userId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您只能更新自己的信息!");
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "没有要更新的数据");
        }
        // 判断 user 和 oldUser 是否一致, 如果一致, 直接返回 1
        if (EntityUtil.entityEq(userUpdateRequest, oldUser)) {
            return 1;
        }
        User updateUser = new User();
        BeanUtils.copyProperties(userUpdateRequest, updateUser);
        return userMapper.updateById(updateUser);
    }

    /**
     * 获取最匹配的用户
     *
     * @param num       - 推荐的数量
     * @param loginUser - 当前登录用户
     * @return 和当前登录用户最匹配的 num 个其他用户
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 获取当前登录用户的标签数据
        String loginUserTags = loginUser.getTags();
        if (StringUtils.isBlank(loginUserTags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前登录用户未设置标签");
        }

        // 查询 loginUserTags 不为空且不是当前登录用户的所有其他用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "userAccount", "username", "avatarUrl", "gender", "tags", "profile", "phone"
                , "email");
        userQueryWrapper.ne("id", loginUser.getId());
        userQueryWrapper.isNotNull("tags");
        List<User> userList = this.list(userQueryWrapper);

        Map<String, List<Integer>> loginUserTagMap = getTagWeightList(loginUserTags);
        LinkedUtil linkedUtil = new LinkedUtil(num);
        for (User user : userList) {
            String userTags = user.getTags();
            if (StringUtils.isBlank(userTags)) {
                continue;
            }
            Map<String, List<Integer>> otherUserTagMap = getTagWeightList(user.getTags());
            double similarity = AlgorithmUtil.similarity(loginUserTagMap, otherUserTagMap);
            linkedUtil.add(user, similarity);
        }
        List<User> userListResult = linkedUtil.getList();
        userListResult.forEach(user -> {
            user.setTags(this.getTagListJson(user));
            this.getSafetyUser(user);
        });
        return userListResult;
    }

    @Override
    public Map<String, List<Integer>> getTagWeightList(String loginUserTags) {
        Map<String, List<TagPair>> loginUserTagPairMap = JsonUtil.jsonToTagPairMap(loginUserTags);
        Map<String, List<Integer>> loginUserTagMap = new HashMap<>(loginUserTagPairMap.size());
        loginUserTagPairMap.forEach((key, value) -> {
            List<Integer> tagWeights = new ArrayList<>();
            for (TagPair tagPair : value) {
                tagWeights.add(tagPair.getWeights());
            }
            loginUserTagMap.put(key, tagWeights);
        });
        return loginUserTagMap;
    }

    /**
     * 用户脱敏
     *
     * @param originUser: 原始用户
     * @return 经过信息脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    /**
     * 从 Session 中获取当前登录用户并判断其是否为管理员
     *
     * @return 如果当前登录用户是管理员, 返回 true; 反之, 返回 false.
     */
    @Override
    public boolean isAdmin(HttpServletRequest httpServletRequest) {
        // 仅管理员可查询
        Object userObject = httpServletRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObject;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 判断当前登录用户是否是管理员
     *
     * @param loginUser - 当前登录用户
     * @return 如果当前登录用户是管理员, 返回 true; 反之, 返回 false.
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public String getTagListJson(User user) {
        Map<String, List<TagPair>> tagPairMap = JsonUtil.jsonToTagPairMap(user.getTags());
        if (tagPairMap == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        tagPairMap.forEach((key, value) -> {
            for (TagPair tagPair : value) {
                sb.append("\"").append(tagPair.getTag()).append("\"").append(",");
            }
        });
        sb.delete(sb.length() - 1, sb.length());
        sb.append("]");
        return sb.toString();
    }

}




