package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.constant.AdConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.dto.AdCreateDTO;
import com.bilbil.dto.ModerationResult;
import com.bilbil.entity.Ad;
import com.bilbil.entity.User;
import com.bilbil.entity.VideoCategory;
import com.bilbil.mapper.AdMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.mapper.VideoMapper;
import com.bilbil.storage.FileStorageService;
import com.bilbil.vo.AdVO;
import com.bilbil.vo.CategoryStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 广告服务：商家发布/管理广告，兴趣定向投放 + CPM 计费
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

    private final AdMapper adMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final ContentModerationService moderationService;

    private void requireMerchant(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无商家权限");
        }
    }

    /** 发布广告（需商家，余额须覆盖预算） */
    public AdVO create(Long userId, AdCreateDTO dto) {
        requireMerchant(userId);
        if (dto.getCpm() == null || dto.getCpm() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "CPM 必须为正数");
        }
        if (dto.getBudget() == null || dto.getBudget() < dto.getCpm()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预算至少需覆盖一次 CPM");
        }
        User user = userMapper.selectById(userId);
        long balance = user.getBalance() == null ? 0 : user.getBalance();
        if (balance < dto.getBudget()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "积分余额不足，请先充值");
        }
        // AI 内容审核（文字 + 广告图）；不通过则驳回（未配 key 时内部自动放行）
        ModerationResult moderation = moderationService.moderateAd(dto.getTitle(), dto.getCoverUrl());
        if (!moderation.passed()) {
            throw new BusinessException(ResultCode.CONTENT_REJECTED,
                    "内容审核未通过：" + StrUtil.blankToDefault(moderation.reason(), "疑似违规内容"));
        }
        Ad ad = Ad.builder()
                .userId(userId)
                .title(dto.getTitle())
                .coverUrl(dto.getCoverUrl())
                .targetUrl(dto.getTargetUrl())
                .categoryId(dto.getCategoryId())
                .cpm(dto.getCpm())
                .budget(dto.getBudget())
                .build();
        adMapper.insert(ad);
        return toVO(adMapper.selectById(ad.getId()));
    }

    /** 我的广告列表 */
    public PageResult<AdVO> myList(Long userId, int page, int size) {
        requireMerchant(userId);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = adMapper.countByUserId(userId);
        List<AdVO> list = adMapper.selectByUserId(userId, (p - 1) * s, s).stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResult.of(total, p, s, list);
    }

    /** 更新广告状态（暂停/继续/结束，仅本人） */
    public void updateStatus(Long userId, Long adId, Integer status) {
        requireMerchant(userId);
        if (!isValidStatus(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法状态");
        }
        Ad ad = adMapper.selectById(adId);
        if (ad == null || !ad.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该广告");
        }
        adMapper.updateStatus(adId, status);
    }

    /** 删除广告（仅本人） */
    public void delete(Long userId, Long adId) {
        requireMerchant(userId);
        Ad ad = adMapper.selectById(adId);
        if (ad == null || !ad.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该广告");
        }
        adMapper.deleteById(adId);
    }

    /**
     * 兴趣定向投放（公开接口）：
     * 取用户偏好分区作为目标分区，返回最多 2 条投放中的广告并记曝光扣费；
     * 定向无结果或未登录时回退到任意投放中的广告。
     */
    public List<AdVO> serve(Long userId) {
        List<Long> categoryIds = null;
        if (userId != null) {
            List<CategoryStatVO> stats = videoMapper.selectUserCategoryStats(userId);
            if (stats != null && !stats.isEmpty()) {
                categoryIds = stats.stream().limit(3)
                        .map(CategoryStatVO::getCategoryId).collect(Collectors.toList());
            }
        }
        List<Ad> ads = adMapper.selectActiveForServing(categoryIds, 5);
        if (ads.isEmpty() && categoryIds != null) {
            ads = adMapper.selectActiveForServing(null, 5);
        }
        List<AdVO> result = new ArrayList<>();
        for (Ad ad : ads) {
            chargeImpression(ad);
            result.add(toVO(adMapper.selectById(ad.getId())));
        }
        return result;
    }

    /** 广告点击（仅作 CTR 指标，不额外扣费） */
    public void click(Long adId) {
        adMapper.incrementClick(adId);
    }

    /** 上传广告图（需商家），返回可访问 URL */
    public String uploadCover(Long userId, MultipartFile file) {
        requireMerchant(userId);
        try {
            String ext = StrUtil.blankToDefault(
                    file.getOriginalFilename() != null
                            ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1)
                            : "jpg", "jpg");
            String fileId = fileStorageService.upload(file.getBytes(), ext);
            return fileStorageService.getAccessUrl(fileId);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    // ---------- 后台管理 ----------

    public PageResult<AdVO> adminList(Integer status, String keyword, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = adMapper.count(status, keyword);
        List<Ad> ads = adMapper.selectList(status, keyword, (p - 1) * s, s);
        Map<Long, User> userMap = buildUserMap(ads);
        List<AdVO> list = ads.stream().map(a -> {
            AdVO vo = toVO(a);
            User u = userMap.get(a.getUserId());
            if (u != null) {
                vo.setMerchantName(StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(total, p, s, list);
    }

    public void adminUpdateStatus(Long adId, Integer status) {
        if (!isValidStatus(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法状态");
        }
        adMapper.updateStatus(adId, status);
    }

    // ---------- 内部方法 ----------

    private boolean isValidStatus(Integer status) {
        return status != null && (status == AdConstant.STATUS_RUNNING
                || status == AdConstant.STATUS_PAUSED || status == AdConstant.STATUS_ENDED);
    }

    private Map<Long, User> buildUserMap(List<Ad> ads) {
        List<Long> uids = ads.stream().map(Ad::getUserId).distinct().collect(Collectors.toList());
        if (uids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(uids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    /** 记一次曝光；满 1000 次按 CPM 扣费，余额不足则暂停，预算耗尽则结束 */
    private void chargeImpression(Ad ad) {
        long old = ad.getImpressions() == null ? 0 : ad.getImpressions();
        adMapper.incrementImpression(ad.getId());
        long newImp = old + 1;
        long charge = (newImp / 1000 - old / 1000) * ad.getCpm();
        if (charge <= 0) {
            return;
        }
        User user = userMapper.selectById(ad.getUserId());
        long balance = user.getBalance() == null ? 0 : user.getBalance();
        if (balance < charge) {
            adMapper.updateStatus(ad.getId(), AdConstant.STATUS_PAUSED);
            return;
        }
        adMapper.incrementSpent(ad.getId(), charge);
        userMapper.addBalance(ad.getUserId(), -charge);
        Ad fresh = adMapper.selectById(ad.getId());
        if (fresh.getSpent() >= fresh.getBudget()) {
            adMapper.updateStatus(ad.getId(), AdConstant.STATUS_ENDED);
        }
    }

    private AdVO toVO(Ad ad) {
        VideoCategory c = categoryService.getById(ad.getCategoryId());
        return AdVO.builder()
                .id(ad.getId())
                .userId(ad.getUserId())
                .title(ad.getTitle())
                .coverUrl(ad.getCoverUrl())
                .targetUrl(ad.getTargetUrl())
                .categoryId(ad.getCategoryId())
                .categoryName(c == null ? null : c.getName())
                .cpm(ad.getCpm())
                .budget(ad.getBudget())
                .impressions(ad.getImpressions())
                .clicks(ad.getClicks())
                .spent(ad.getSpent())
                .status(ad.getStatus())
                .createTime(ad.getCreateTime())
                .build();
    }
}
