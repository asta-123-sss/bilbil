package com.bilbil.common.exception;

import com.bilbil.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>业务逻辑中主动抛出的异常，由全局异常处理器统一捕获并返回给前端</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
