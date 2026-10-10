package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 投币模块接口
 *
 * <p>投币数由 Redis Set 原子维护，投币关系持久化到 video_coin 表，
 * 计数由定时任务批量回写。</p>
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    /**
     * 投币
     *
     * <p><code>POST /api/video/{videoId}/coin</code>，需登录。</p>
     *
     * @return 投币后最新投币数
     */
    @PostMapping("/{videoId}/coin")
    public Result<Long> coin(@PathVariable Long videoId) {
        return Result.success(coinService.coin(videoId, UserContext.requireUserId()));
    }

    /**
     * 取消投币
     *
     * <p><code>DELETE /api/video/{videoId}/coin</code>，需登录。</p>
     *
     * @return 取消后最新投币数
     */
    @DeleteMapping("/{videoId}/coin")
    public Result<Long> uncoin(@PathVariable Long videoId) {
        return Result.success(coinService.uncoin(videoId, UserContext.requireUserId()));
    }
}
