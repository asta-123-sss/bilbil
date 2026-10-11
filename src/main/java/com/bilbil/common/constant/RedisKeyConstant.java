package com.bilbil.common.constant;

/**
 * Redis Key 常量统一管理
 * <p>所有 Redis key 集中在此，避免散落各处造成 key 冲突或难以维护</p>
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    /** 登录会话 token：token:{token} -> userId */
    public static final String TOKEN_KEY = "bilbil:token:";

    /** 热点视频基础信息缓存：video:info:{videoId} -> VideoVO JSON */
    public static final String VIDEO_INFO_KEY = "bilbil:video:info:";

    /** 视频播放量计数：video:play:{videoId} -> count */
    public static final String VIDEO_PLAY_COUNT_KEY = "bilbil:video:play:";

    /** 视频点赞用户集合：video:like:set:{videoId} -> Set<userId> */
    public static final String VIDEO_LIKE_SET_KEY = "bilbil:video:like:set:";

    /** 视频收藏用户集合：video:collect:set:{videoId} -> Set<userId> */
    public static final String VIDEO_COLLECT_SET_KEY = "bilbil:video:collect:set:";

    /** 视频投币用户集合：video:coin:set:{videoId} -> Set<userId> */
    public static final String VIDEO_COIN_SET_KEY = "bilbil:video:coin:set:";

    /** 用户关注集合：user:follow:set:{userId} -> Set<followUserId> */
    public static final String USER_FOLLOW_SET_KEY = "bilbil:user:follow:set:";

    /** 用户粉丝集合：user:fan:set:{userId} -> Set<followerUserId> */
    public static final String USER_FAN_SET_KEY = "bilbil:user:fan:set:";

    /** 视频分区列表缓存：category:list -> List<VideoCategory> JSON */
    public static final String CATEGORY_LIST_KEY = "bilbil:category:list";

    /** 视频布隆过滤器（防缓存穿透）：bloom:video -> bitmap */
    public static final String VIDEO_BLOOM_KEY = "bilbil:bloom:video";

    /** 计数待回写视频集合：video:dirty -> Set<videoId>（计数变更后加入，定时任务批量回写DB） */
    public static final String VIDEO_DIRTY_SET_KEY = "bilbil:video:dirty";

    /** 用户上传分片已传序号集合：video:chunk:set:{uploadId} -> Set<chunkIndex>（辅助查询） */
    public static final String VIDEO_CHUNK_SET_KEY = "bilbil:video:chunk:set:";

    /** 分片上传会话元数据：video:upload:{uploadId} -> JSON（fileName/fileSize/chunkCount/fileMd5） */
    public static final String UPLOAD_META_KEY = "bilbil:video:upload:";

    /** 用户广告智能排序缓存：ad:rank:{userId} -> 有序 adId 列表 JSON */
    public static final String AD_RANK_KEY = "bilbil:ad:rank:";

    /** 生成 token key */
    public static String tokenKey(String token) {
        return TOKEN_KEY + token;
    }

    /** 视频信息缓存 key */
    public static String videoInfoKey(Long videoId) {
        return VIDEO_INFO_KEY + videoId;
    }

    /** 播放量计数 key */
    public static String videoPlayCountKey(Long videoId) {
        return VIDEO_PLAY_COUNT_KEY + videoId;
    }

    /** 点赞用户集合 key */
    public static String videoLikeSetKey(Long videoId) {
        return VIDEO_LIKE_SET_KEY + videoId;
    }

    /** 收藏用户集合 key */
    public static String videoCollectSetKey(Long videoId) {
        return VIDEO_COLLECT_SET_KEY + videoId;
    }

    /** 投币用户集合 key */
    public static String videoCoinSetKey(Long videoId) {
        return VIDEO_COIN_SET_KEY + videoId;
    }

    /** 关注集合 key */
    public static String userFollowSetKey(Long userId) {
        return USER_FOLLOW_SET_KEY + userId;
    }

    /** 粉丝集合 key */
    public static String userFanSetKey(Long userId) {
        return USER_FAN_SET_KEY + userId;
    }

    /** 分片上传会话元数据 key */
    public static String uploadMetaKey(String uploadId) {
        return UPLOAD_META_KEY + uploadId;
    }

    /** 广告智能排序缓存 key */
    public static String adRankKey(Long userId) {
        return AD_RANK_KEY + userId;
    }
}
