package com.bilbil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家积分充值请求参数
 */
@Data
public class RechargeDTO {

    /** 充值积分数（正数） */
    @NotNull(message = "充值积分数不能为空")
    private Long amount;
}
