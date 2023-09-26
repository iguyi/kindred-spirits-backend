package com.guyi.kindredspirits.exception;

import com.guyi.kindredspirits.common.ErrorCode;

/**
 * 自定义异常类
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1688591093535371380L;

    /**
     * 错误码
     */
    private final int code;

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 错误信息描述(详情)
     */
    private final String description;

    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = description;
    }
}
