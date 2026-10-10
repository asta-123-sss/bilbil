package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.service.FollowService;
import com.bilbil.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 关注模块接口
 *
 * <p>关注/粉丝关系由 Redis Set 原子维护（计数以 Set 大小为准），数据库 user_follow 表持久化。</p>
 */
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 关注用户
     *
     * <p><code>POST /api/follow/{userId}</code>，需登录，userId 为被关注用户ID。</p>
     */
    @PostMapping("/{userId}")
    public Result<Void> follow(@PathVariable Long userId) {
        followService.follow(UserContext.requireUserId(), userId);
        return Result.success();
    }

    /**
     * 取消关注
     *
     * <p><code>DELETE /api/follow/{userId}</code>，需登录，userId 为被关注用户ID。</p>
     */
    @DeleteMapping("/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId) {
        followService.unfollow(UserContext.requireUserId(), userId);
        return Result.success();
    }

    /**
     * 某用户的关注列表（分页）
     *
     * <p><code>GET /api/follow/{userId}/followings</code>，需登录，query 参数：page、size。</p>
     */
    @GetMapping("/{userId}/followings")
    public Result<PageResult<UserVO>> followings(@PathVariable Long userId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(followService.followings(userId, page, size));
    }

    /**
     * 某用户的粉丝列表（分页）
     *
     * <p><code>GET /api/follow/{userId}/fans</code>，需登录，query 参数：page、size。</p>
     */
    @GetMapping("/{userId}/fans")
    public Result<PageResult<UserVO>> fans(@PathVariable Long userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(followService.fans(userId, page, size));
    }
}
