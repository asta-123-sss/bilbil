package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布广告请求参数
 */
@Data
public class AdCreateDTO {

    /** 广告标题 */
    @NotBlank(message = "广告标题不能为空")
    private String title;

    /** 广告图URL（可空） */
    private String coverUrl;

    /** 落地页链接 */
    @NotBlank(message = "落地页链接不能为空")
    private String targetUrl;

    /** 目标分区（兴趣定向） */
    @NotNull(message = "目标分区不能为空")
    private Long categoryId;

    /** 每千次曝光价格（积分） */
    @NotNull(message = "CPM不能为空")
    private Long cpm;

    /** 总预算（积分） */
    @NotNull(message = "预算不能为空")
    private Long budget;
}
