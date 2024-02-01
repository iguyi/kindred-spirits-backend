package com.guyi.kindredspirits.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T> 返回参数对象的类型
 * @author 孤诣
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = -1446590250555886882L;

    /**
     * 处理状态码
     */
    private int code;

    /**
     * 返回对象参数
     */
    private T data;

    /**
     * 处理信息
     */
    private String message;

    /**
     * 状态码描述(详情)
     */
    private String description;

    public BaseResponse() {
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    public BaseResponse(int code, T data, String message) {
        this(code, data, message, "");
    }

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMsg(), errorCode.getDescription());
    }

}
