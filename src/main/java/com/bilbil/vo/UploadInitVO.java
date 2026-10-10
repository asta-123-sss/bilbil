package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 视频分片上传初始化返回对象
 */
@Data
@Builder
public class UploadInitVO implements Serializable {

    /** 上传会话ID（后续分片上传、合并都要携带） */
    private String uploadId;

    /** 分片大小（字节） */
    private Long chunkSize;

    /** 分片总数 */
    private Integer chunkCount;

    /** 已上传的分片序号（断点续传：客户端只重传未上传的分片） */
    private List<Integer> uploadedIndexes;

    /** 是否秒传成功（整个文件 MD5 已存在，无需上传） */
    private Boolean alreadyExists;

    /** 秒传成功时返回已存在的视频ID */
    private Long videoId;
}
