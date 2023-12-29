package com.guyi.kindredspirits.common.contant;

/**
 * 用户常量
 *
 * @author 孤诣
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    /**
     * 重点用户标记
     */
    int HOT_USER_TAG = 1;

    /**
     * 推荐相似用户的最大数量
     */
    int MATCH_NUM = 20;

    /**
     * 用户最小账号常量
     */
    int USER_ACCOUNT_MIN = 4;

    /**
     * 用户密码最小长度
     */
    int USER_PASSWORD_MIN = 8;

    /**
     * 用户昵称最大长度
     */
    int USERNAME_MAX = 10;

    /**
     * 随机用户昵称前缀
     */
    String DEFAULT_USERNAME_PRE = "user_";

}
