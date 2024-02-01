package com.guyi.kindredspirits.exception;

import com.guyi.kindredspirits.common.BaseResponse;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 利用 Spring AOP 处理全局异常
 *
 * @author 孤诣
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 只捕获 BusinessException 类型的异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Object> businessException(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<Object> runtimeException(RuntimeException e) {
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }

}
