package com.bilbil.service;

import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.User;
import com.bilbil.entity.UserFollow;
import com.bilbil.mapper.UserFollowMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 关注服务
 * <p>关注/粉丝关系使用 Redis Set 缓存维护（原子操作），数据库 user_follow 表持久化，
 * 关注数/粉丝数通过 Redis Set 大小获取，Set 缺失时从数据库回填。</p>
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final NotifyService notifyService;

    /**
     * 关注用户
     *
     * @param userId   关注者（当前登录用户）
     * @param followId 被关注者
     */
    public void follow(Long userId, Long followId) {
        if (userId.equals(followId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己");
        }
        if (userMapper.selectById(followId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 幂等：已关注直接返回
        if (isFollowing(userId, followId)) {
            return;
        }
        // 1. 持久化关注关系
        userFollowMapper.insert(UserFollow.builder().userId(userId).followId(followId).build());
        // 2. 更新 Redis 关注/粉丝集合（原子）
        redisTemplate.opsForSet().add(RedisKeyConstant.userFollowSetKey(userId), String.valueOf(followId));
        redisTemplate.opsForSet().add(RedisKeyConstant.userFanSetKey(followId), String.valueOf(userId));
        // 3. 异步发送关注通知
        notifyService.notifyFollow(followId, userId);
    }

    /**
     * 取消关注
     */
    public void unfollow(Long userId, Long followId) {
        userFollowMapper.delete(userId, followId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.userFollowSetKey(userId), String.valueOf(followId));
        redisTemplate.opsForSet().remove(RedisKeyConstant.userFanSetKey(followId), String.valueOf(userId));
    }

    /**
     * 判断是否已关注
     */
    public boolean isFollowing(Long userId, Long followId) {
        if (userId == null || followId == null) {
            return false;
        }
        String key = RedisKeyConstant.userFollowSetKey(userId);
        backfillIfAbsent(userId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(followId)));
    }

    /**
     * 关注数
     */
    public long followCount(Long userId) {
        String key = RedisKeyConstant.userFollowSetKey(userId);
        backfillIfAbsent(userId);
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /**
     * 粉丝数
     */
    public long fanCount(Long userId) {
        String key = RedisKeyConstant.userFanSetKey(userId);
        backfillIfAbsent(userId);
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /**
     * 分页查询关注列表
     */
    public PageResult<UserVO> followings(Long userId, int page, int size) {
        long total = followCount(userId);
        int offset = (page - 1) * size;
        List<UserVO> list = userFollowMapper.selectFollowings(userId, offset, size)
                .stream().map(this::toVO).toList();
        return PageResult.of(total, page, size, list);
    }

    /**
     * 分页查询粉丝列表
     */
    public PageResult<UserVO> fans(Long userId, int page, int size) {
        long total = fanCount(userId);
        int offset = (page - 1) * size;
        List<UserVO> list = userFollowMapper.selectFans(userId, offset, size)
                .stream().map(this::toVO).toList();
        return PageResult.of(total, page, size, list);
    }

    /** 关注/粉丝集合不存在时从数据库回填（处理缓存击穿/重启丢失） */
    private void backfillIfAbsent(Long userId) {
        String followKey = RedisKeyConstant.userFollowSetKey(userId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(followKey))) {
            List<Long> followIds = userFollowMapper.selectFollowIds(userId);
            if (!followIds.isEmpty()) {
                redisTemplate.opsForSet().add(followKey, followIds.stream().map(String::valueOf).toArray(String[]::new));
            }
            redisTemplate.expire(followKey, Duration.ofDays(7));
        }
        String fanKey = RedisKeyConstant.userFanSetKey(userId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(fanKey))) {
            List<Long> fanIds = userFollowMapper.selectFanIds(userId);
            if (!fanIds.isEmpty()) {
                redisTemplate.opsForSet().add(fanKey, fanIds.stream().map(String::valueOf).toArray(String[]::new));
            }
            redisTemplate.expire(fanKey, Duration.ofDays(7));
        }
    }

    /** User 实体转 UserVO（列表场景，不含关注/粉丝数） */
    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .signature(user.getSignature())
                .build();
    }
}
