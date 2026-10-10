package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.MerchantReviewDTO;
import com.bilbil.dto.RechargeDTO;
import com.bilbil.dto.StatusDTO;
import com.bilbil.service.AdminService;
import com.bilbil.vo.AdVO;
import com.bilbil.vo.CommentVO;
import com.bilbil.vo.MerchantApplyVO;
import com.bilbil.vo.UserVO;
import com.bilbil.vo.VideoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理接口（仅管理员）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 视频列表（审核）
     *
     * <p><code>GET /api/admin/videos</code>，需管理员，query：status、keyword、page、size。</p>
     */
    @GetMapping("/videos")
    public Result<PageResult<VideoVO>> videos(@RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listVideos(UserContext.requireUserId(), status, keyword, page, size));
    }

    /**
     * 视频上架/下架
     *
     * <p><code>PUT /api/admin/video/{id}/status</code>，需管理员，body：{ "status": 1上架 2下架 }。</p>
     */
    @PutMapping("/video/{id}/status")
    public Result<Void> reviewVideo(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminService.reviewVideo(UserContext.requireUserId(), id, dto.getStatus());
        return Result.success();
    }

    /**
     * 用户列表（管理）
     *
     * <p><code>GET /api/admin/users</code>，需管理员，query：keyword、page、size。</p>
     */
    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listUsers(UserContext.requireUserId(), keyword, page, size));
    }

    /**
     * 用户封禁/解封
     *
     * <p><code>PUT /api/admin/user/{id}/status</code>，需管理员，body：{ "status": 0解封 1封禁 }。</p>
     */
    @PutMapping("/user/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminService.setUserStatus(UserContext.requireUserId(), id, dto.getStatus());
        return Result.success();
    }

    /**
     * 评论列表（管理）
     *
     * <p><code>GET /api/admin/comments</code>，需管理员，query：keyword、page、size。</p>
     */
    @GetMapping("/comments")
    public Result<PageResult<CommentVO>> comments(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listComments(UserContext.requireUserId(), keyword, page, size));
    }

    /**
     * 删除违规评论（软删除）
     *
     * <p><code>DELETE /api/admin/comment/{id}</code>，需管理员。</p>
     */
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        adminService.deleteComment(UserContext.requireUserId(), id);
        return Result.success();
    }

    /**
     * 商家申请列表
     *
     * <p><code>GET /api/admin/merchant-applies</code>，需管理员，query：status、keyword、page、size。</p>
     */
    @GetMapping("/merchant-applies")
    public Result<PageResult<MerchantApplyVO>> merchantApplies(@RequestParam(required = false) Integer status,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listMerchantApplies(UserContext.requireUserId(), status, keyword, page, size));
    }

    /**
     * 审核商家申请
     *
     * <p><code>PUT /api/admin/merchant-apply/{id}/review</code>，需管理员，body：{ "status": 1通过 2拒绝, "note": "..." }。</p>
     */
    @PutMapping("/merchant-apply/{id}/review")
    public Result<Void> reviewMerchantApply(@PathVariable Long id, @Valid @RequestBody MerchantReviewDTO dto) {
        adminService.reviewMerchantApply(UserContext.requireUserId(), id, dto.getStatus(), dto.getNote());
        return Result.success();
    }

    /**
     * 商家列表（含积分余额）
     *
     * <p><code>GET /api/admin/merchants</code>，需管理员，query：keyword、page、size。</p>
     */
    @GetMapping("/merchants")
    public Result<PageResult<UserVO>> merchants(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listMerchants(UserContext.requireUserId(), keyword, page, size));
    }

    /**
     * 商家积分充值
     *
     * <p><code>PUT /api/admin/merchant/{id}/recharge</code>，需管理员，body：{ "amount": 100 }。</p>
     */
    @PutMapping("/merchant/{id}/recharge")
    public Result<Void> rechargeMerchant(@PathVariable Long id, @Valid @RequestBody RechargeDTO dto) {
        adminService.rechargeMerchant(UserContext.requireUserId(), id, dto.getAmount());
        return Result.success();
    }

    /**
     * 广告列表
     *
     * <p><code>GET /api/admin/ads</code>，需管理员，query：status、keyword、page、size。</p>
     */
    @GetMapping("/ads")
    public Result<PageResult<AdVO>> ads(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAds(UserContext.requireUserId(), status, keyword, page, size));
    }

    /**
     * 广告状态管理（下架/暂停/恢复）
     *
     * <p><code>PUT /api/admin/ad/{id}/status</code>，需管理员，body：{ "status": 1投放中 2暂停 3结束 }。</p>
     */
    @PutMapping("/ad/{id}/status")
    public Result<Void> adStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminService.updateAdStatus(UserContext.requireUserId(), id, dto.getStatus());
        return Result.success();
    }
}
