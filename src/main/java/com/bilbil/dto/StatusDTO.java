package com.bilbil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态修改请求参数（后台审核：视频上架/下架、用户封禁/解封）
 */
@Data
public class StatusDTO {

    /** 目标状态值（视频：1上架 2下架；用户：0解封 1封禁） */
    @NotNull(message = "状态值不能为空")
    private Integer status;
}
