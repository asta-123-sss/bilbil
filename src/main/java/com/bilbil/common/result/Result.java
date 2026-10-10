package com.bilbil.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果体
 * <p>所有接口返回结构统一为 {code, msg, data}</p>
 *
 * @param <T> 数据泛型
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 返回数据 */
    private T data;

    private Result() {
    }

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    /** 成功（携带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 成功（自定义提示 + 数据） */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /** 失败（默认系统错误） */
    public static <T> Result<T> fail() {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMsg(), null);
    }

    /** 失败（指定提示信息） */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), msg, null);
    }

    /** 失败（指定状态码 + 提示信息） */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /** 失败（指定状态码枚举） */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(), null);
    }
}
