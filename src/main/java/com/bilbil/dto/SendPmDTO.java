package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私信请求参数
 */
@Data
public class SendPmDTO {

    /** 接收者用户ID */
    @NotNull(message = "接收者不能为空")
    private Long toUserId;

    /** 私信内容 */
    @NotBlank(message = "私信内容不能为空")
    @Size(max = 500, message = "私信内容不能超过500字")
    private String content;
}
