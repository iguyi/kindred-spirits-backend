package com.guyi.kindredspirits.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.UserUpdateRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author 孤诣
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userPassword  - 密码
     * @param checkPassword - 校验密码
     * @return 返回此处操作中注册用户的数量, 正常应该是 1
     */
    long userRegister(String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount        - 用户账号
     * @param userPassword       - 用户密码
     * @param httpServletRequest - httpServletRequest
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest);

    /**
     * 将 JSON 格式的用户标签数据反序列化为 Java 对象
     *
     * @param loginUserTags - JSON 格式的用户标签数据
     * @return 用户标签反序列化后得到的 Java 对象
     */
    Map<String, List<Integer>> getTagWeightList(String loginUserTags);

    /**
     * 用户脱敏
     *
     * @param originUser - 原始用户
     * @return 经过信息脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 根据标签搜索用户 -- 内存查询
     *
     * @param tagNameList - 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 根据标签搜索用户 -- SQL 查询
     *
     * @param tagNameList - 标签列表, 被搜索用户需要有的标签
     * @return 符合要求的用户
     */
    List<User> searchUsersByTagsBySql(List<String> tagNameList);

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest - 用户的新信息
     * @param loginUser         - 当前登录用户
     * @return 更改的数据总量, 正常应该是 1
     */
    int updateUser(UserUpdateRequest userUpdateRequest, User loginUser);


    /**
     * 从 Session 中获取当前登录用户信息, 并判断是否登录
     *
     * @param httpServletRequest - httpServletRequest
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest httpServletRequest);

    /**
     * 从 Session 中获取当前登录用户并判断其是否为管理员
     *
     * @param httpServletRequest - httpServletRequest
     * @return 如果当前登录用户是管理员, 返回 true; 反之, 返回 false.
     */
    boolean isAdmin(HttpServletRequest httpServletRequest);

    /**
     * 判断用户是否是管理员, 通常是用于对当前登录用户的的权限校验
     *
     * @param user - 用户, 通常是当前登录用户
     * @return 返回 true 表示当前登录用户是管理员
     */
    boolean isAdmin(User user);

    /**
     * 获取最匹配的用户
     *
     * @param num       - 推荐的数量
     * @param loginUser - 当前登录用户
     * @return 和当前登录用户最匹配的 num 个其他用户
     */
    List<User> matchUsers(long num, User loginUser);

    /**
     * user 中原始的 tags 数据转换为 ["tag-1", "tag-2"] 的格式。
     * 原始的 tag 数据格式如下:
     * {
     * "1": [
     * {"tag": "Java", "weights": 1}
     * ]
     * }
     *
     * @param user - 用户对象
     * @return tags 经过转换的 user
     */
    String getTagListJson(User user);
}
