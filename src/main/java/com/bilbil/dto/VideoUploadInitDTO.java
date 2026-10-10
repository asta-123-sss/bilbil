package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 视频分片上传初始化请求参数（开始分块上传前先初始化，获取上传会话）
 */
@Data
public class VideoUploadInitDTO {

    /** 原始文件名 */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 文件总大小（字节） */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须为正数")
    private Long fileSize;

    /** 单个分片大小（字节） */
    @NotNull(message = "分片大小不能为空")
    @Positive(message = "分片大小必须为正数")
    private Long chunkSize;

    /** 分片总数 */
    @NotNull(message = "分片数量不能为空")
    @Positive(message = "分片数量必须为正数")
    private Integer chunkCount;

    /** 整个文件 MD5（选填，用于秒传去重） */
    private String fileMd5;
}
