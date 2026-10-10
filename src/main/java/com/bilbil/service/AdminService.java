package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.User;
import com.bilbil.mapper.UserMapper;
import com.bilbil.vo.AdVO;
import com.bilbil.vo.CommentVO;
import com.bilbil.vo.MerchantApplyVO;
import com.bilbil.vo.UserVO;
import com.bilbil.vo.VideoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台审核服务（管理员）
 *
 * <p>所有操作前先校验当前用户是否为管理员（user.role == 1）。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final VideoService videoService;
    private final CommentService commentService;
    private final MerchantService merchantService;
    private final AdService adService;

    /** 校验管理员权限 */
    private void requireAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无管理员权限");
        }
    }

    /** 视频列表（可按状态/关键字筛选） */
    public PageResult<VideoVO> listVideos(Long userId, Integer status, String keyword, int page, int size) {
        requireAdmin(userId);
        return videoService.adminList(status, keyword, page, size);
    }

    /** 视频上架/下架 */
    public void reviewVideo(Long userId, Long videoId, Integer status) {
        requireAdmin(userId);
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态取值不合法（1上架 2下架）");
        }
        videoService.adminUpdateStatus(videoId, status);
    }

    /** 用户列表（按关键字筛选） */
    public PageResult<UserVO> listUsers(Long userId, String keyword, int page, int size) {
        requireAdmin(userId);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = userMapper.count(keyword);
        List<User> users = userMapper.selectList(keyword, (p - 1) * s, s);
        List<UserVO> list = users.stream().map(u -> UserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(StrUtil.blankToDefault(u.getNickname(), u.getUsername()))
                .avatar(u.getAvatar())
                .status(u.getStatus())
                .role(u.getRole())
                .createTime(u.getCreateTime())
                .build()).toList();
        return PageResult.of(total, p, s, list);
    }

    /** 用户封禁/解封 */
    public void setUserStatus(Long userId, Long targetId, Integer status) {
        requireAdmin(userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态取值不合法（0解封 1封禁）");
        }
        if (userMapper.selectById(targetId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.updateStatus(targetId, status);
    }

    /** 评论列表（按关键字筛选） */
    public PageResult<CommentVO> listComments(Long userId, String keyword, int page, int size) {
        requireAdmin(userId);
        return commentService.adminList(keyword, page, size);
    }

    /** 删除违规评论 */
    public void deleteComment(Long userId, Long commentId) {
        requireAdmin(userId);
        commentService.adminDelete(commentId);
    }

    /** 商家申请列表 */
    public PageResult<MerchantApplyVO> listMerchantApplies(Long userId, Integer status, String keyword, int page, int size) {
        requireAdmin(userId);
        return merchantService.adminList(status, keyword, page, size);
    }

    /** 审核商家申请（1通过 2拒绝） */
    public void reviewMerchantApply(Long userId, Long applyId, Integer result, String note) {
        requireAdmin(userId);
        if (result == null || (result != 1 && result != 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果取值不合法（1通过 2拒绝）");
        }
        merchantService.adminReview(applyId, result == 1, note);
    }

    /** 商家列表（含积分余额） */
    public PageResult<UserVO> listMerchants(Long userId, String keyword, int page, int size) {
        requireAdmin(userId);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = userMapper.countMerchants(keyword);
        List<User> users = userMapper.selectMerchants(keyword, (p - 1) * s, s);
        List<UserVO> list = users.stream().map(u -> UserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(StrUtil.blankToDefault(u.getNickname(), u.getUsername()))
                .avatar(u.getAvatar())
                .status(u.getStatus())
                .role(u.getRole())
                .balance(u.getBalance())
                .createTime(u.getCreateTime())
                .build()).toList();
        return PageResult.of(total, p, s, list);
    }

    /** 商家积分充值 */
    public void rechargeMerchant(Long userId, Long merchantId, Long amount) {
        requireAdmin(userId);
        if (amount == null || amount <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "充值积分数必须为正数");
        }
        if (userMapper.selectById(merchantId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.addBalance(merchantId, amount);
    }

    /** 广告列表 */
    public PageResult<AdVO> listAds(Long userId, Integer status, String keyword, int page, int size) {
        requireAdmin(userId);
        return adService.adminList(status, keyword, page, size);
    }

    /** 广告状态管理（下架/暂停/恢复） */
    public void updateAdStatus(Long userId, Long adId, Integer status) {
        requireAdmin(userId);
        adService.adminUpdateStatus(adId, status);
    }
}
