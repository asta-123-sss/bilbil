package com.bilbil.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 观看历史返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistoryVO implements Serializable {

    /** 视频ID */
    private Long videoId;
    /** 视频标题 */
    private String title;
    /** 封面图 URL */
    private String coverUrl;
    /** 视频时长（秒） */
    private Integer duration;
    /** 上次看到第几秒 */
    private Integer progress;
    /** 最近观看时间 */
    private LocalDateTime watchTime;
    /** 作者ID */
    private Long authorId;
    /** 作者昵称 */
    private String authorName;
    /** 播放量 */
    private Long playCount;
}
