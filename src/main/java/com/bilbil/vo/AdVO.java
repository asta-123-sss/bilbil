package com.bilbil.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 广告返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdVO implements Serializable {

    private Long id;
    private Long userId;
    private String title;
    private String coverUrl;
    private String targetUrl;
    private Long categoryId;
    private String categoryName;
    private Long cpm;
    private Long budget;
    private Long impressions;
    private Long clicks;
    private Long spent;
    /** 1投放中 2已暂停 3已结束 */
    private Integer status;
    private LocalDateTime createTime;

    /** 商家名称（仅后台管理列表回填） */
    private String merchantName;
}
