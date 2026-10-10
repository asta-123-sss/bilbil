package com.bilbil.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.config.BilbilProperties;
import com.bilbil.dto.ChangePasswordDTO;
import com.bilbil.dto.LoginDTO;
import com.bilbil.dto.RegisterDTO;
import com.bilbil.dto.UpdateUserDTO;
import com.bilbil.entity.User;
import com.bilbil.mapper.UserMapper;
import com.bilbil.storage.FileStorageService;
import com.bilbil.vo.LoginVO;
import com.bilbil.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * 用户服务：注册、登录（Redis token 会话）、信息修改、头像上传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final FileStorageService fileStorageService;
    private final FollowService followService;
    private final BilbilProperties properties;

    /**
     * 注册
     */
    public UserVO register(RegisterDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = User.builder()
                .username(dto.getUsername())
                .password(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()))
                .nickname(StrUtil.blankToDefault(dto.getNickname(), dto.getUsername()))
                .email(dto.getEmail())
                .gender(0)
                .status(0)
                .build();
        userMapper.insert(user);
        return toVO(user, null);
    }

    /**
     * 登录：校验账号密码，生成 token 存入 Redis
     */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        // 生成 token，token -> userId 存入 Redis（有效期可配置）
        String token = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(RedisKeyConstant.tokenKey(token), String.valueOf(user.getId()),
                Duration.ofDays(properties.getToken().getExpireDays()));
        return LoginVO.builder().token(token).user(toVO(user, user.getId())).build();
    }

    /**
     * 退出登录：删除 Redis token
     */
    public void logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            redisTemplate.delete(RedisKeyConstant.tokenKey(token));
        }
    }

    /**
     * 查询当前登录用户信息
     */
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(user, userId);
    }

    /**
     * 修改用户信息
     */
    public UserVO updateInfo(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.update(User.builder()
                .id(userId)
                .nickname(dto.getNickname())
                .gender(dto.getGender())
                .birthday(dto.getBirthday())
                .signature(dto.getSignature())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build());
        return toVO(userMapper.selectById(userId), userId);
    }

    /**
     * 修改密码：校验旧密码，成功后更新为新密码
     */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "旧密码不正确");
        }
        userMapper.updatePassword(userId, BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
    }

    /**
     * 上传头像，返回头像 URL
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        try {
            String ext = StrUtil.blankToDefault(
                    file.getOriginalFilename() != null
                            ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1)
                            : "jpg", "jpg");
            String fileId = fileStorageService.upload(file.getBytes(), ext);
            String url = fileStorageService.getAccessUrl(fileId);
            userMapper.update(User.builder().id(userId).avatar(url).build());
            return url;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 查询用户主页信息（含关注/粉丝数、是否已关注）
     */
    public UserVO getUserProfile(Long targetUserId, Long currentUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(user, currentUserId);
    }

    /** User 实体转 UserVO */
    private UserVO toVO(User user, Long currentUserId) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .signature(user.getSignature())
                .followCount(followService.followCount(user.getId()))
                .fanCount(followService.fanCount(user.getId()))
                .role(user.getRole())
                .balance(currentUserId != null && currentUserId.equals(user.getId()) ? user.getBalance() : null)
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .followed(currentUserId == null || currentUserId.equals(user.getId()) ? null
                        : followService.isFollowing(currentUserId, user.getId()))
                .build();
    }
}
