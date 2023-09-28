package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.contant.UserConstant;
import com.guyi.kindredspirits.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 张仕恒
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2023-07-22 21:03:08
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户名称
     * @param userPassword  密码
     * @param checkPassword 校验密码
     * @return 返回此处操作中注册用户的数量, 正常应该是 1
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest);

    /**
     * 用户脱敏
     *
     * @param originUser: 原始用户
     * @return 经过信息脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 根据标签搜索用户 -- 内存查询
     *
     * @param tagNameList: 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 根据标签搜索用户 -- SQL 查询
     *
     * @param tagNameList: 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    List<User> searchUsersByTagsBySQL(List<String> tagNameList);

    /**
     * 更新用户信息
     *
     * @param user      - 用户的新信息
     * @param loginUser - 当前登录用户
     * @return 更改的数据总量, 正常应该是 1
     */
    int updateUser(User user, User loginUser);


    /**
     * 从 Session 中获取当前登录用户信息, 并判断是否登录
     *
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest httpServletRequest);

    /**
     * 从 Session 中获取当前登录用户并判断其是否为管理员
     *
     * @return 如果当前登录用户是管理员, 返回 true; 反之, 返回 false.
     */
    boolean isAdmin(HttpServletRequest httpServletRequest);

    /**
     * 判断当前登录用户是否是管理员
     *
     * @param loginUser - 当前登录用户
     * @return 如果当前登录用户是管理员, 返回 true; 反之, 返回 false.
     */
    boolean isAdmin(User loginUser);
}
