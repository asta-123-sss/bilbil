package com.bilbil.common.result;

import lombok.Getter;

/**
 * 统一返回状态码枚举
 * <p>约定：200 成功；4xx 客户端错误；5xx 服务端错误</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 用户模块 1xxx
    USERNAME_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被封禁"),

    // 视频模块 2xxx
    VIDEO_NOT_FOUND(2001, "视频不存在"),
    VIDEO_CHUNK_NOT_COMPLETE(2002, "视频分片不完整，请先上传全部分片"),
    VIDEO_CHUNK_EXISTS(2003, "该分片已上传"),
    UPLOAD_TASK_NOT_FOUND(2004, "上传任务不存在"),
    VIDEO_STATUS_ERROR(2005, "视频状态不允许该操作"),

    // 互动模块 3xxx
    COMMENT_NOT_FOUND(3001, "评论不存在"),

    // 审核模块 4xxx
    CONTENT_REJECTED(4001, "内容审核未通过"),

    // 系统错误 5xxx
    FILE_UPLOAD_ERROR(5001, "文件上传失败"),
    FILE_DELETE_ERROR(5002, "文件删除失败"),
    INTERNAL_ERROR(5000, "系统内部错误，请稍后重试");

    /** 状态码 */
    private final Integer code;
    /** 提示信息 */
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
