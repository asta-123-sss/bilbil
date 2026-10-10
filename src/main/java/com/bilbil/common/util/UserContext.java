package com.bilbil.common.util;

/**
 * 当前登录用户上下文
 * <p>基于 ThreadLocal 保存当前请求的登录用户ID，
 * 由 {@link com.bilbil.interceptor.LoginInterceptor} 在请求进入时写入，请求结束时清理，避免线程池复用导致数据串号</p>
 */
public final class UserContext {

    private UserContext() {
    }

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /** 设置当前登录用户ID */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /** 获取当前登录用户ID（未登录返回 null） */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 获取当前登录用户ID，未登录抛异常 */
    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new com.bilbil.common.exception.BusinessException(
                    com.bilbil.common.result.ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 清理（必须配合 finally 调用） */
    public static void clear() {
        USER_ID.remove();
    }
}
