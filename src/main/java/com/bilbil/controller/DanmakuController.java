package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.DanmakuPublishDTO;
import com.bilbil.entity.Danmaku;
import com.bilbil.service.DanmakuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 弹幕模块接口
 *
 * <p>弹幕存储于 MySQL（danmaku 表），播放时按视频进度时间区间分段加载。</p>
 */
@RestController
@RequestMapping("/api/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    /**
     * 发送弹幕
     *
     * <p><code>POST /api/danmaku/send</code>，需登录。</p>
     */
    @PostMapping("/send")
    public Result<Danmaku> send(@Valid @RequestBody DanmakuPublishDTO dto) {
        return Result.success(danmakuService.send(UserContext.requireUserId(), dto));
    }

    /**
     * 按时间区间查询弹幕
     *
     * <p><code>GET /api/danmaku/list</code>，公开接口，query 参数：videoId、from、to（单位秒）。
     * 不传 from/to 时默认返回 0~300 秒区间。</p>
     */
    @GetMapping("/list")
    public Result<List<Danmaku>> list(@RequestParam Long videoId,
                                      @RequestParam(required = false) Integer from,
                                      @RequestParam(required = false) Integer to) {
        return Result.success(danmakuService.listByTimeRange(videoId, from, to));
    }
}
