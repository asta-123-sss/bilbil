package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.ChangePasswordDTO;
import com.bilbil.dto.LoginDTO;
import com.bilbil.dto.RegisterDTO;
import com.bilbil.dto.UpdateUserDTO;
import com.bilbil.service.UserService;
import com.bilbil.vo.LoginVO;
import com.bilbil.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块接口
 *
 * <p>包含注册、登录、退出登录、个人信息管理、头像上传、用户主页查询。</p>
 * <p>除注册/登录/主页外，其余接口均需在请求头携带 <code>token</code>（登录接口返回）。</p>
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     *
     * <p><code>POST /api/user/register</code></p>
     *
     * @param dto 注册信息（用户名 3-20、密码 6-32、昵称/邮箱选填）
     * @return 注册后的用户信息
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    /**
     * 用户登录
     *
     * <p><code>POST /api/user/login</code></p>
     *
     * @param dto 登录信息（用户名 + 密码）
     * @return 登录凭证 token 与用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 退出登录（删除 Redis 中的 token 会话）
     *
     * <p><code>POST /api/user/logout</code>，请求头：<code>token</code></p>
     *
     * @param token 登录凭证
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "token", required = false) String token) {
        userService.logout(token);
        return Result.success();
    }

    /**
     * 查询当前登录用户信息
     *
     * <p><code>GET /api/user/me</code>，需登录</p>
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser(UserContext.requireUserId()));
    }

    /**
     * 修改当前用户信息（昵称/性别/生日/签名/邮箱/手机号）
     *
     * <p><code>PUT /api/user/info</code>，需登录</p>
     */
    @PutMapping("/info")
    public Result<UserVO> updateInfo(@Valid @RequestBody UpdateUserDTO dto) {
        return Result.success(userService.updateInfo(UserContext.requireUserId(), dto));
    }

    /**
     * 上传头像
     *
     * <p><code>POST /api/user/avatar</code>，需登录，multipart/form-data，字段名 <code>file</code></p>
     *
     * @param file 头像图片
     * @return 头像访问 URL
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(UserContext.requireUserId(), file));
    }

    /**
     * 修改密码
     *
     * <p><code>PUT /api/user/password</code>，需登录，body：{ oldPassword, newPassword }。</p>
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.requireUserId(), dto);
        return Result.success();
    }

    /**
     * 查询指定用户主页信息（含关注/粉丝数、当前用户是否已关注）
     *
     * <p><code>GET /api/user/profile/{userId}</code>，公开接口（未登录时 followed 字段为空）</p>
     *
     * @param userId 目标用户ID
     */
    @GetMapping("/profile/{userId}")
    public Result<UserVO> profile(@PathVariable Long userId) {
        return Result.success(userService.getUserProfile(userId, UserContext.getUserId()));
    }
}
