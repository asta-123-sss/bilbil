package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.result.PageResult;
import com.bilbil.entity.User;
import com.bilbil.entity.WatchHistory;
import com.bilbil.mapper.UserMapper;
import com.bilbil.mapper.WatchHistoryMapper;
import com.bilbil.vo.WatchHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 观看历史服务
 */
@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private final WatchHistoryMapper watchHistoryMapper;
    private final UserMapper userMapper;

    /**
     * 记录观看历史（同一视频重复观看会刷新进度与时间）
     */
    public void record(Long userId, Long videoId, Integer progress) {
        int p = progress == null || progress < 0 ? 0 : progress;
        watchHistoryMapper.upsert(WatchHistory.builder()
                .userId(userId).videoId(videoId).progress(p).build());
    }

    /**
     * 分页查询观看历史
     */
    public PageResult<WatchHistoryVO> list(Long userId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        long total = watchHistoryMapper.countByUserId(userId);
        List<WatchHistoryVO> list = watchHistoryMapper.selectByUserId(userId, (p - 1) * s, s);

        // 批量加载作者昵称
        List<Long> authorIds = list.stream().map(WatchHistoryVO::getAuthorId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(authorIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        list.forEach(vo -> {
            User author = userMap.get(vo.getAuthorId());
            if (author != null) {
                vo.setAuthorName(StrUtil.blankToDefault(author.getNickname(), author.getUsername()));
            }
        });
        return PageResult.of(total, p, s, list);
    }

    /**
     * 删除单条历史
     */
    public void remove(Long userId, Long videoId) {
        watchHistoryMapper.deleteByUserAndVideo(userId, videoId);
    }

    /**
     * 清空历史
     */
    public void clear(Long userId) {
        watchHistoryMapper.clearAll(userId);
    }
}
