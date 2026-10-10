package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 视频分片合并 + 元数据提交请求参数
 */
@Data
public class VideoMergeDTO {

    /** 上传会话ID（分片上传初始化时获得） */
    @NotBlank(message = "上传会话ID不能为空")
    private String uploadId;

    /** 视频标题 */
    @NotBlank(message = "视频标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    /** 视频简介 */
    @Size(max = 1000, message = "简介长度不能超过1000")
    private String description;

    /** 所属分区ID */
    @NotNull(message = "分区不能为空")
    private Long categoryId;

    /** 标签（逗号分隔） */
    @Size(max = 500, message = "标签长度不能超过500")
    private String tags;

    /** 封面图 URL（先调用封面上传接口获得） */
    private String coverUrl;

    /** 可见权限：0公开 1仅粉丝 2私密 */
    private Integer visibility;

    /** 视频时长（秒，选填，客户端可计算） */
    private Integer duration;
}
