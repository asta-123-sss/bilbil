package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 观看历史实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory implements Serializable {

    /** 主键 */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 视频ID */
    private Long videoId;
    /** 上次看到第几秒 */
    private Integer progress;
    /** 首次观看时间 */
    private LocalDateTime createTime;
    /** 最近观看时间 */
    private LocalDateTime updateTime;
}
