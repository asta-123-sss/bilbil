package com.bilbil.task;

import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.entity.Video;
import com.bilbil.mapper.VideoMapper;
import com.bilbil.service.CoinService;
import com.bilbil.service.CollectService;
import com.bilbil.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 计数异步落库定时任务
 * <p>点赞/收藏/播放计数先写入 Redis（原子操作），本任务定期将变更过的视频计数批量回写 MySQL，
 * 避免高并发互动时频繁写库。互动发生时会把 videoId 加入 Redis 的 dirty 集合标记为待回写。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountFlushTask {

    private final StringRedisTemplate redisTemplate;
    private final VideoMapper videoMapper;
    private final LikeService likeService;
    private final CollectService collectService;
    private final CoinService coinService;

    /**
     * 每 30 秒执行一次：批量回写待回写视频的播放量/点赞数/收藏数
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void flushCounts() {
        Set<String> dirtyIds = redisTemplate.opsForSet().members(RedisKeyConstant.VIDEO_DIRTY_SET_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }
        int success = 0;
        for (String idStr : dirtyIds) {
            Long videoId;
            try {
                videoId = Long.valueOf(idStr);
            } catch (NumberFormatException e) {
                continue;
            }
            try {
                Video video = videoMapper.selectById(videoId);
                if (video == null) {
                    continue;
                }
                long playCount = readPlayCount(videoId, video);
                long likeCount = likeService.getLikeCount(videoId);
                long collectCount = collectService.getCollectCount(videoId);
                long coinCount = coinService.getCoinCount(videoId);
                videoMapper.updateCounts(videoId, playCount, likeCount, collectCount, coinCount);
                success++;
            } catch (Exception e) {
                log.error("计数回写失败 videoId={}", videoId, e);
            }
        }
        // 清理已处理的 dirty 标记
        redisTemplate.delete(RedisKeyConstant.VIDEO_DIRTY_SET_KEY);
        log.info("计数回写完成：处理 {} 个视频", success);
    }

    private long readPlayCount(Long videoId, Video video) {
        String cached = redisTemplate.opsForValue().get(RedisKeyConstant.videoPlayCountKey(videoId));
        return cached == null ? (video.getPlayCount() == null ? 0 : video.getPlayCount()) : Long.parseLong(cached);
    }
}
