package com.bilbil.common.constant;

/**
 * 广告投放相关状态常量
 */
public final class AdConstant {

    private AdConstant() {
    }

    /** 广告状态：投放中 */
    public static final int STATUS_RUNNING = 1;
    /** 广告状态：已暂停 */
    public static final int STATUS_PAUSED = 2;
    /** 广告状态：已结束 */
    public static final int STATUS_ENDED = 3;

    /** 商家申请状态：待审核 */
    public static final int APPLY_PENDING = 0;
    /** 商家申请状态：已通过 */
    public static final int APPLY_APPROVED = 1;
    /** 商家申请状态：已拒绝 */
    public static final int APPLY_REJECTED = 2;
}
