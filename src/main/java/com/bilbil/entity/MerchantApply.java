package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家申请实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApply implements Serializable {

    /** 申请ID */
    private Long id;
    /** 申请用户ID */
    private Long userId;
    /** 姓名/公司名 */
    private String realName;
    /** 联系方式 */
    private String contact;
    /** 申请理由 */
    private String reason;
    /** 状态：0待审核 1通过 2拒绝 */
    private Integer status;
    /** 审核备注 */
    private String reviewNote;
    /** 申请时间 */
    private LocalDateTime createTime;
    /** 审核时间 */
    private LocalDateTime reviewTime;
}
