package com.guyi.kindredspirits.service;

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
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount        用户账号
     * @param userPassword       用户密码
     * @param httpServletRequest
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
     * 根据标签搜索用户 -- SQL 查询
     *
     * @param tagNameList: 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    List<User> sqlSearchUsersByTags(List<String> tagNameList);

    List<User> memorySearchUsersByTags(List<String> tagNameList);
}
