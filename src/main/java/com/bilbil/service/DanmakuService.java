package com.bilbil.service;

import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.dto.DanmakuPublishDTO;
import com.bilbil.entity.Danmaku;
import com.bilbil.entity.Video;
import com.bilbil.mapper.DanmakuMapper;
import com.bilbil.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 弹幕服务（弹幕存储 MySQL，按视频时间区间分段加载）
 */
@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final DanmakuMapper danmakuMapper;
    private final VideoMapper videoMapper;

    /**
     * 发送弹幕
     */
    public Danmaku send(Long userId, DanmakuPublishDTO dto) {
        Video video = videoMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        Danmaku danmaku = Danmaku.builder()
                .videoId(dto.getVideoId())
                .userId(userId)
                .content(dto.getContent())
                .videoTime(dto.getVideoTime())
                .color(dto.getColor() == null ? "#FFFFFF" : dto.getColor())
                .mode(dto.getMode() == null ? 0 : dto.getMode())
                .fontSize(dto.getFontSize() == null ? 25 : dto.getFontSize())
                .status(0)
                .build();
        danmakuMapper.insert(danmaku);
        videoMapper.increaseDanmakuCount(dto.getVideoId());
        return danmaku;
    }

    /**
     * 按时间区间查询弹幕（B站按视频进度分段加载）
     */
    public List<Danmaku> listByTimeRange(Long videoId, Integer from, Integer to) {
        int start = from == null ? 0 : from;
        int end = to == null ? start + 300 : to;   // 默认返回 5 分钟区间
        return danmakuMapper.selectByTimeRange(videoId, start, end);
    }
}
