package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.AdCreateDTO;
import com.bilbil.dto.StatusDTO;
import com.bilbil.service.AdService;
import com.bilbil.vo.AdVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 广告接口：商家自助管理 + 公开投放
 */
@RestController
@RequestMapping("/api/ad")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    /** 发布广告（商家） */
    @PostMapping
    public Result<AdVO> create(@Valid @RequestBody AdCreateDTO dto) {
        return Result.success(adService.create(UserContext.requireUserId(), dto));
    }

    /** 我的广告列表（商家） */
    @GetMapping("/my")
    public Result<PageResult<AdVO>> my(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(adService.myList(UserContext.requireUserId(), page, size));
    }

    /** 更新广告状态（商家，暂停/继续/结束） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adService.updateStatus(UserContext.requireUserId(), id, dto.getStatus());
        return Result.success();
    }

    /** 删除广告（商家） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adService.delete(UserContext.requireUserId(), id);
        return Result.success();
    }

    /** 上传广告图（商家） */
    @PostMapping("/cover")
    public Result<String> uploadCover(@RequestPart("file") MultipartFile file) {
        return Result.success(adService.uploadCover(UserContext.requireUserId(), file));
    }

    /** 信息流广告（公开，兴趣定向投放） */
    @GetMapping("/feed")
    public Result<List<AdVO>> feed() {
        return Result.success(adService.serve(UserContext.getUserId()));
    }

    /** 广告点击（公开） */
    @PostMapping("/{id}/click")
    public Result<Void> click(@PathVariable Long id) {
        adService.click(id);
        return Result.success();
    }
}
