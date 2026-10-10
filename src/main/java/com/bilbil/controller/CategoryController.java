package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.entity.VideoCategory;
import com.bilbil.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 视频分区接口
 *
 * <p>分区列表变化频率极低，接口返回的是 Redis 缓存数据（缓存未命中时回源数据库并重建缓存）。</p>
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询全部分区列表
     *
     * <p><code>GET /api/category/list</code>，公开接口。</p>
     */
    @GetMapping("/list")
    public Result<List<VideoCategory>> list() {
        return Result.success(categoryService.list());
    }
}
