package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 弹幕实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Danmaku implements Serializable {

    /** 弹幕ID */
    private Long id;
    /** 所属视频ID */
    private Long videoId;
    /** 发送弹幕用户ID */
    private Long userId;
    /** 弹幕内容 */
    private String content;
    /** 弹幕出现时间点（秒） */
    private Integer videoTime;
    /** 弹幕颜色（十六进制） */
    private String color;
    /** 弹幕模式：0滚动 1顶部 2底部 */
    private Integer mode;
    /** 字体大小 */
    private Integer fontSize;
    /** 状态：0正常 1已屏蔽 */
    private Integer status;
    /** 发送时间 */
    private LocalDateTime createTime;
}
