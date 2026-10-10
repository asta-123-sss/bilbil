package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.service.CollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏模块接口
 *
 * <p>收藏数由 Redis Set 原子维护，收藏关系持久化到 video_collect 表，
 * 计数由定时任务批量回写 video 表。</p>
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;

    /**
     * 收藏视频
     *
     * <p><code>POST /api/video/{videoId}/collect</code>，需登录。</p>
     *
     * @return 收藏后最新收藏数
     */
    @PostMapping("/{videoId}/collect")
    public Result<Long> collect(@PathVariable Long videoId) {
        return Result.success(collectService.collect(videoId, UserContext.requireUserId()));
    }

    /**
     * 取消收藏
     *
     * <p><code>DELETE /api/video/{videoId}/collect</code>，需登录。</p>
     *
     * @return 取消后最新收藏数
     */
    @DeleteMapping("/{videoId}/collect")
    public Result<Long> uncollect(@PathVariable Long videoId) {
        return Result.success(collectService.uncollect(videoId, UserContext.requireUserId()));
    }
}
