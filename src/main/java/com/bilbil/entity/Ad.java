package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 广告实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad implements Serializable {

    /** 广告ID */
    private Long id;
    /** 商家用户ID */
    private Long userId;
    /** 广告标题 */
    private String title;
    /** 广告图URL */
    private String coverUrl;
    /** 落地页链接 */
    private String targetUrl;
    /** 目标分区（兴趣定向） */
    private Long categoryId;
    /** 每千次曝光价格（积分） */
    private Long cpm;
    /** 总预算（积分） */
    private Long budget;
    /** 累计曝光次数 */
    private Long impressions;
    /** 累计点击次数 */
    private Long clicks;
    /** 已消耗积分 */
    private Long spent;
    /** 状态：1投放中 2已暂停 3已结束 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
