package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频分片实体（分块上传/断点续传）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoChunk implements Serializable {

    /** 分片记录ID */
    private Long id;
    /** 分片上传会话标识 */
    private String uploadId;
    /** 合并完成后关联的视频ID */
    private Long videoId;
    /** 分片序号（从0开始） */
    private Integer chunkIndex;
    /** 分片大小（字节） */
    private Long chunkSize;
    /** 分片 MD5 */
    private String chunkMd5;
    /** 分片文件存储ID */
    private String fileId;
    /** 状态：0已上传 1已合并 2已清理 */
    private Integer status;
    /** 上传时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
