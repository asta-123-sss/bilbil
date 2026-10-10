package com.bilbil.service;

import com.bilbil.common.constant.AdConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.dto.MerchantApplyDTO;
import com.bilbil.entity.MerchantApply;
import com.bilbil.entity.User;
import com.bilbil.mapper.MerchantApplyMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.vo.MerchantApplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商家服务：商家申请（用户提交 / 管理员审核）
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantApplyMapper merchantApplyMapper;
    private final UserMapper userMapper;

    /** 用户提交商家申请（仅普通用户可申请） */
    public void apply(Long userId, MerchantApplyDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getRole() != null && user.getRole() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已是商家或管理员，无需重复申请");
        }
        MerchantApply latest = merchantApplyMapper.selectLatestByUserId(userId);
        if (latest != null && latest.getStatus() != null && latest.getStatus() == AdConstant.APPLY_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已有待审核的申请，请耐心等待");
        }
        merchantApplyMapper.insert(MerchantApply.builder()
                .userId(userId)
                .realName(dto.getRealName())
                .contact(dto.getContact())
                .reason(dto.getReason())
                .build());
    }

    /** 查询我的最新一条申请 */
    public MerchantApply myApply(Long userId) {
        return merchantApplyMapper.selectLatestByUserId(userId);
    }

    /** 后台：商家申请列表 */
    public PageResult<MerchantApplyVO> adminList(Integer status, String keyword, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = merchantApplyMapper.count(status, keyword);
        List<MerchantApplyVO> list = merchantApplyMapper.selectList(status, keyword, (p - 1) * s, s);
        return PageResult.of(total, p, s, list);
    }

    /** 后台：审核商家申请（通过 → 设为商家角色并赠初始积分） */
    public void adminReview(Long applyId, boolean approve, String note) {
        MerchantApply apply = merchantApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != AdConstant.APPLY_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已处理");
        }
        if (approve) {
            merchantApplyMapper.updateStatus(applyId, AdConstant.APPLY_APPROVED, note);
            userMapper.updateRole(apply.getUserId(), 2); // 2 = 商家
            userMapper.addBalance(apply.getUserId(), 100L); // 赠送 100 积分
        } else {
            merchantApplyMapper.updateStatus(applyId, AdConstant.APPLY_REJECTED, note);
        }
    }
}
