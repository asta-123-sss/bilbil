package com.bilbil.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家申请返回对象（含申请人账号信息，后台审核用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApplyVO implements Serializable {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String realName;
    private String contact;
    private String reason;
    /** 0待审核 1通过 2拒绝 */
    private Integer status;
    private String reviewNote;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
