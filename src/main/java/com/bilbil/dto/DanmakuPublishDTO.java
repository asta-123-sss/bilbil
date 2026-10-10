package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 弹幕发送请求参数
 */
@Data
public class DanmakuPublishDTO {

    /** 所属视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /** 弹幕内容 */
    @NotBlank(message = "弹幕内容不能为空")
    @Size(max = 500, message = "弹幕内容不能超过500字")
    private String content;

    /** 弹幕出现时间点（秒） */
    @NotNull(message = "弹幕时间点不能为空")
    private Integer videoTime;

    /** 弹幕颜色（默认白色） */
    private String color = "#FFFFFF";

    /** 弹幕模式：0滚动 1顶部 2底部 */
    private Integer mode = 0;

    /** 字体大小 */
    private Integer fontSize = 25;
}
