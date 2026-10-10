package com.bilbil.mq;

/**
 * RocketMQ Topic / Tag 常量
 */
public final class MqConstant {

    private MqConstant() {
    }

    /** 视频相关消息 Topic（转码任务） */
    public static final String TOPIC_VIDEO = "bilbil-video-topic";

    /** 消息通知 Topic（点赞/评论/关注/回复） */
    public static final String TOPIC_NOTIFY = "bilbil-notify-topic";

    /** 视频转码 Tag */
    public static final String TAG_TRANSCODE = "transcode";

    /** 点赞通知 Tag */
    public static final String TAG_LIKE = "like";

    /** 评论通知 Tag */
    public static final String TAG_COMMENT = "comment";

    /** 关注通知 Tag */
    public static final String TAG_FOLLOW = "follow";

    /** 回复通知 Tag */
    public static final String TAG_REPLY = "reply";
}
