package com.bilbil.service;

import com.bilbil.common.constant.LikeStatusConstant;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.Video;
import com.bilbil.entity.VideoCoin;
import com.bilbil.mapper.VideoCoinMapper;
import com.bilbil.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频投币服务
 * <p>投币为「仅计数开关」：不涉及用户余额，投币数通过 Redis Set 原子维护，
 * 投币关系持久化到 video_coin 表，计数由定时任务批量回写 video.coin_count。</p>
 */
@Service
@RequiredArgsConstructor
public class CoinService {

    private final VideoMapper videoMapper;
    private final VideoCoinMapper videoCoinMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 投币
     *
     * @return 投币后总数
     */
    public long coin(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        // 幂等：已投币直接返回当前数量
        if (isCoined(videoId, userId)) {
            return getCoinCount(videoId);
        }
        // 1. Redis 集合原子添加（防并发重复计数）
        redisTemplate.opsForSet().add(RedisKeyConstant.videoCoinSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        // 2. 关系表 upsert
        videoCoinMapper.insert(VideoCoin.builder()
                .userId(userId).videoId(videoId).status(LikeStatusConstant.ACTIVE).build());
        return getCoinCount(videoId);
    }

    /**
     * 取消投币
     *
     * @return 取消后总数
     */
    public long uncoin(Long videoId, Long userId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.videoCoinSetKey(videoId), String.valueOf(userId));
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));
        videoCoinMapper.updateStatus(userId, videoId, LikeStatusConstant.CANCELED);
        return getCoinCount(videoId);
    }

    /**
     * 当前用户是否已投币
     */
    public boolean isCoined(Long videoId, Long userId) {
        if (userId == null) {
            return false;
        }
        String key = RedisKeyConstant.videoCoinSetKey(videoId);
        backfillIfAbsent(videoId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(userId)));
    }

    /**
     * 投币总数（优先 Redis 集合大小）
     */
    public long getCoinCount(Long videoId) {
        String key = RedisKeyConstant.videoCoinSetKey(videoId);
        backfillIfAbsent(videoId);
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /** 集合缺失时从投币表回填（处理 Redis 重启/缓存丢失） */
    private void backfillIfAbsent(Long videoId) {
        String key = RedisKeyConstant.videoCoinSetKey(videoId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            List<Long> userIds = videoCoinMapper.selectUserIdsByVideoId(videoId);
            if (!userIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, userIds.stream().map(String::valueOf).toArray(String[]::new));
            }
        }
    }
}
