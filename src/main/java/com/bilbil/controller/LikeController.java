package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞模块接口
 *
 * <p>点赞数由 Redis Set 原子维护（SADD/SREM），不直接在 video 表自增，
 * 点赞关系持久化到 video_like 表，计数由定时任务批量回写。</p>
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 点赞视频
     *
     * <p><code>POST /api/video/{videoId}/like</code>，需登录。</p>
     *
     * @return 点赞后最新点赞数
     */
    @PostMapping("/{videoId}/like")
    public Result<Long> like(@PathVariable Long videoId) {
        return Result.success(likeService.like(videoId, UserContext.requireUserId()));
    }

    /**
     * 取消点赞
     *
     * <p><code>DELETE /api/video/{videoId}/like</code>，需登录。</p>
     *
     * @return 取消后最新点赞数
     */
    @DeleteMapping("/{videoId}/like")
    public Result<Long> unlike(@PathVariable Long videoId) {
        return Result.success(likeService.unlike(videoId, UserContext.requireUserId()));
    }
}
