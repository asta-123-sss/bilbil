package com.bilbil.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 观看进度上报请求参数
 */
@Data
public class WatchProgressDTO {

    /** 当前播放到的秒数 */
    @Min(value = 0, message = "进度不能为负数")
    private Integer progress = 0;
}
