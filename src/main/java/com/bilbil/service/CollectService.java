package com.bilbil.service;

import com.bilbil.common.constant.LikeStatusConstant;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.Video;
import com.bilbil.entity.VideoCollect;
import com.bilbil.mapper.VideoCollectMapper;
import com.bilbil.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频收藏服务
 * <p>收藏数通过 Redis Set 原子维护，收藏关系持久化到 video_collect 表。</p>
 */
@Service
@RequiredArgsConstructor
public class CollectService {

    private final VideoMapper videoMapper;
    private final VideoCollectMapper videoCollectMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 收藏
     *
     * @return 收藏后总数
     */
    public long collect(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        if (isCollected(videoId, userId)) {
            return getCollectCount(videoId);
        }
        redisTemplate.opsForSet().add(RedisKeyConstant.videoCollectSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        videoCollectMapper.insert(VideoCollect.builder()
                .userId(userId).videoId(videoId).status(LikeStatusConstant.ACTIVE).build());
        return getCollectCount(videoId);
    }

    /**
     * 取消收藏
     *
     * @return 取消后总数
     */
    public long uncollect(Long videoId, Long userId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.videoCollectSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        videoCollectMapper.updateStatus(userId, videoId, LikeStatusConstant.CANCELED);
        return getCollectCount(videoId);
    }

    /**
     * 当前用户是否已收藏
     */
    public boolean isCollected(Long videoId, Long userId) {
        if (userId == null) {
            return false;
        }
        String key = RedisKeyConstant.videoCollectSetKey(videoId);
        backfillIfAbsent(videoId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(userId)));
    }

    /**
     * 收藏总数
     */
    public long getCollectCount(Long videoId) {
        String key = RedisKeyConstant.videoCollectSetKey(videoId);
        backfillIfAbsent(videoId);
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /** 集合缺失时从收藏表回填 */
    private void backfillIfAbsent(Long videoId) {
        String key = RedisKeyConstant.videoCollectSetKey(videoId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            List<Long> userIds = videoCollectMapper.selectUserIdsByVideoId(videoId);
            if (!userIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, userIds.stream().map(String::valueOf).toArray(String[]::new));
            }
        }
    }
}
