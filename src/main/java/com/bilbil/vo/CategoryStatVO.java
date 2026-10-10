package com.bilbil.vo;

import lombok.Data;

/**
 * 用户分类偏好统计（用于个性化推荐）
 */
@Data
public class CategoryStatVO {

    /** 分区ID */
    private Long categoryId;

    /** 偏好权重分（越大越偏好） */
    private Long score;
}
