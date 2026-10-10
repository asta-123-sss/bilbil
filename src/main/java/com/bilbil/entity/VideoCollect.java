package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频收藏实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCollect implements Serializable {

    /** 主键 */
    private Long id;
    /** 收藏用户ID */
    private Long userId;
    /** 被收藏视频ID */
    private Long videoId;
    /** 状态：0已取消 1已收藏 */
    private Integer status;
    /** 收藏时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
