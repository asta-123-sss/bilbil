package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家申请请求参数
 */
@Data
public class MerchantApplyDTO {

    /** 姓名/公司名 */
    @NotBlank(message = "姓名/公司名不能为空")
    private String realName;

    /** 联系方式 */
    @NotBlank(message = "联系方式不能为空")
    private String contact;

    /** 申请理由 */
    @NotBlank(message = "申请理由不能为空")
    private String reason;
}
