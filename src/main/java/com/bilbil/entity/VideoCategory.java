package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频分区实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCategory implements Serializable {

    /** 分区ID */
    private Long id;
    /** 分区名称 */
    private String name;
    /** 分区描述 */
    private String description;
    /** 排序权重（越小越靠前） */
    private Integer sort;
    /** 创建时间 */
    private LocalDateTime createTime;
}
