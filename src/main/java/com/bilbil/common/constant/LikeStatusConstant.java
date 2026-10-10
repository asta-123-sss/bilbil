package com.bilbil.common.constant;

/**
 * 点赞/收藏关系状态常量
 */
public final class LikeStatusConstant {

    private LikeStatusConstant() {
    }

    /** 已取消点赞/收藏 */
    public static final int CANCELED = 0;
    /** 已点赞/已收藏 */
    public static final int ACTIVE = 1;
}
