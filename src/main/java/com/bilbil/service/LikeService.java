package com.bilbil.service;

import com.bilbil.common.constant.LikeStatusConstant;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.Video;
import com.bilbil.entity.VideoLike;
import com.bilbil.mapper.VideoLikeMapper;
import com.bilbil.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频点赞服务
 * <p>点赞数通过 Redis Set 原子维护（SADD/SREM），不直接在 video 表自增，
 * 点赞关系持久化到 video_like 表，计数由定时任务批量回写。</p>
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final VideoMapper videoMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final StringRedisTemplate redisTemplate;
    private final NotifyService notifyService;

    /**
     * 点赞
     *
     * @return 点赞后总数
     */
    public long like(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        // 幂等：已点赞直接返回当前数量
        if (isLiked(videoId, userId)) {
            return getLikeCount(videoId);
        }
        // 1. Redis 集合原子添加（防并发重复计数）
        redisTemplate.opsForSet().add(RedisKeyConstant.videoLikeSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        // 2. 关系表 upsert
        videoLikeMapper.insert(VideoLike.builder()
                .userId(userId).videoId(videoId).status(LikeStatusConstant.ACTIVE).build());
        // 3. 异步通知视频作者
        notifyService.notifyLike(video.getUserId(), userId, videoId);
        return getLikeCount(videoId);
    }

    /**
     * 取消点赞
     *
     * @return 取消后总数
     */
    public long unlike(Long videoId, Long userId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.videoLikeSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        videoLikeMapper.updateStatus(userId, videoId, LikeStatusConstant.CANCELED);
        return getLikeCount(videoId);
    }

    /**
     * 当前用户是否已点赞
     */
    public boolean isLiked(Long videoId, Long userId) {
        if (userId == null) {
            return false;
        }
        String key = RedisKeyConstant.videoLikeSetKey(videoId);
        backfillIfAbsent(videoId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(userId)));
    }

    /**
     * 点赞总数（优先 Redis 集合大小）
     */
    public long getLikeCount(Long videoId) {
        String key = RedisKeyConstant.videoLikeSetKey(videoId);
        backfillIfAbsent(videoId);
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /** 集合缺失时从点赞表回填（处理 Redis 重启/缓存丢失） */
    private void backfillIfAbsent(Long videoId) {
        String key = RedisKeyConstant.videoLikeSetKey(videoId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            List<Long> userIds = videoLikeMapper.selectUserIdsByVideoId(videoId);
            if (!userIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, userIds.stream().map(String::valueOf).toArray(String[]::new));
            }
        }
    }
}
