package com.bilbil.dto;

import lombok.Data;

/**
 * 视频列表查询参数（GET 请求 query 参数绑定）
 */
@Data
public class VideoQueryDTO {

    /** 分区ID */
    private Long categoryId;

    /** 标题关键字 */
    private String keyword;

    /** 排序方式：new 最新 | hot 最热 */
    private String orderBy = "new";

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页大小 */
    private Integer size = 10;
}
