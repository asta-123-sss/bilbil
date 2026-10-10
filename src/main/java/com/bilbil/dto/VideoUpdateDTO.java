package com.bilbil.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 视频信息修改请求参数
 */
@Data
public class VideoUpdateDTO {

    /** 视频标题 */
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    /** 视频简介 */
    @Size(max = 1000, message = "简介长度不能超过1000")
    private String description;

    /** 所属分区ID */
    private Long categoryId;

    /** 标签（逗号分隔） */
    @Size(max = 500, message = "标签长度不能超过500")
    private String tags;

    /** 封面图 URL */
    private String coverUrl;

    /** 可见权限：0公开 1仅粉丝 2私密 */
    private Integer visibility;
}
