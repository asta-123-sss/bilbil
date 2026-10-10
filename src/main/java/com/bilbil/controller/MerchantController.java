package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.MerchantApplyDTO;
import com.bilbil.entity.MerchantApply;
import com.bilbil.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家接口（需登录）
 */
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /** 提交商家申请 */
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody MerchantApplyDTO dto) {
        merchantService.apply(UserContext.requireUserId(), dto);
        return Result.success();
    }

    /** 查询我的最新一条申请 */
    @GetMapping("/apply")
    public Result<MerchantApply> myApply() {
        return Result.success(merchantService.myApply(UserContext.requireUserId()));
    }
}
