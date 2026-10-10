package com.bilbil.common.constant;

/**
 * 视频相关业务常量
 */
public final class VideoConstant {

    private VideoConstant() {
    }

    /**
     * 视频状态
     */
    public static final class Status {
        /** 待审核 */
        public static final int PENDING = 0;
        /** 已发布 */
        public static final int PUBLISHED = 1;
        /** 已下架 */
        public static final int OFF_SHELF = 2;
        /** 转码中 */
        public static final int TRANSCODING = 3;
    }

    /**
     * 视频可见权限
     */
    public static final class Visibility {
        /** 公开 */
        public static final int PUBLIC = 0;
        /** 仅粉丝 */
        public static final int FANS_ONLY = 1;
        /** 私密 */
        public static final int PRIVATE = 2;
    }

    /**
     * 视频分片状态
     */
    public static final class ChunkStatus {
        /** 已上传 */
        public static final int UPLOADED = 0;
        /** 已合并 */
        public static final int MERGED = 1;
        /** 已清理 */
        public static final int CLEANED = 2;
    }
}
