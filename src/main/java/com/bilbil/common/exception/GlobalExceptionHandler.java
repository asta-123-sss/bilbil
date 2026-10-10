package com.bilbil.common.exception;

import com.bilbil.common.result.Result;
import com.bilbil.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>统一捕获业务异常、参数校验异常等，不向前端返回原生堆栈信息</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@RequestBody + @Valid，JSON 请求体）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验异常：{}", msg);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 参数校验异常（表单绑定）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定异常：{}", msg);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 参数校验异常（@RequestParam + @Validated，单个参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数约束异常：{}", msg);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "缺少请求参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getName());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数类型不匹配：" + e.getName());
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超限");
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "上传文件大小超过限制");
    }

    /**
     * 兜底异常：未知系统错误，不向前端暴露堆栈
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
