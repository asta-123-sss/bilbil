package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频点赞实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLike implements Serializable {

    /** 主键 */
    private Long id;
    /** 点赞用户ID */
    private Long userId;
    /** 被点赞视频ID */
    private Long videoId;
    /** 状态：0已取消 1已点赞 */
    private Integer status;
    /** 点赞时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
