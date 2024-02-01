package com.guyi.kindredspirits.common;

/**
 * 错误码
 *
 * @author 孤诣
 */
public enum ErrorCode {

    /**
     * 请求正确处理
     */
    SUCCESS(0, "ok", ""),

    /**
     * 请求参数错误: 参数为空、参数格式错误、参数值不合法
     */
    PARAMS_ERROR(40000, "请求参数错误", ""),

    /**
     * 请求的目标数据在数据库中不存在
     */
    NULL_ERROR(40001, "请求数据为空", ""),

    /**
     * 用户为登录
     */
    NOT_LOGIN(40100, "未登录", ""),

    /**
     * 操作无权限, 比如普通用户操作需要管理员权限的内容
     */
    NO_AUTH(40101, "无权限", ""),

    /**
     * 禁止操作, 比如修改被人的数据
     */
    FORBIDDEN(40300, "禁止操作", ""),

    /**
     * 意想不到的错误
     */
    SYSTEM_ERROR(50000, "系统内部异常", "");


    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态码信息
     */
    private final String msg;

    /**
     * 状态码描述(详情)
     */
    private final String description;

    ErrorCode(int code, String msg, String description) {
        this.code = code;
        this.msg = msg;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getDescription() {
        return description;
    }

}
