package com.bilbil.dto;

/**
 * 内容审核结果
 *
 * @param passed true 通过 / false 不通过
 * @param reason 不通过时的具体原因（通过时为 null）
 */
public record ModerationResult(boolean passed, String reason) {

    public static ModerationResult pass() {
        return new ModerationResult(true, null);
    }

    public static ModerationResult reject(String reason) {
        return new ModerationResult(false, reason);
    }
}
