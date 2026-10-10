package com.bilbil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家申请审核请求参数
 */
@Data
public class MerchantReviewDTO {

    /** 审核结果：1通过 2拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    /** 审核备注（可选） */
    private String note;
}
