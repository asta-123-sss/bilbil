package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.WatchProgressDTO;
import com.bilbil.service.WatchHistoryService;
import com.bilbil.vo.WatchHistoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 观看历史接口
 *
 * <p>播放视频时自动记录历史，暂停/离开页面时上报观看进度。</p>
 */
@RestController
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    /**
     * 分页查询我的观看历史
     *
     * <p><code>GET /api/history</code>，需登录，query 参数：page、size。</p>
     */
    @GetMapping("/api/history")
    public Result<PageResult<WatchHistoryVO>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(watchHistoryService.list(UserContext.requireUserId(), page, size));
    }

    /**
     * 上报观看进度（暂停/离开页面时调用）
     *
     * <p><code>POST /api/video/{videoId}/watch</code>，需登录，body：{ "progress": 秒数 }。</p>
     */
    @PostMapping("/api/video/{videoId}/watch")
    public Result<Void> reportProgress(@PathVariable Long videoId, @Valid @RequestBody WatchProgressDTO dto) {
        watchHistoryService.record(UserContext.requireUserId(), videoId, dto.getProgress());
        return Result.success();
    }

    /**
     * 删除单条历史
     *
     * <p><code>DELETE /api/history/{videoId}</code>，需登录。</p>
     */
    @DeleteMapping("/api/history/{videoId}")
    public Result<Void> remove(@PathVariable Long videoId) {
        watchHistoryService.remove(UserContext.requireUserId(), videoId);
        return Result.success();
    }

    /**
     * 清空全部历史
     *
     * <p><code>DELETE /api/history</code>，需登录。</p>
     */
    @DeleteMapping("/api/history")
    public Result<Void> clear() {
        watchHistoryService.clear(UserContext.requireUserId());
        return Result.success();
    }
}
