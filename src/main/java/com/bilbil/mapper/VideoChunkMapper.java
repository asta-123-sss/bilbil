package com.bilbil.mapper;

import com.bilbil.entity.VideoChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 视频分片表 Mapper（分块上传/断点续传）
 */
@Mapper
public interface VideoChunkMapper {

    /** 新增分片记录 */
    @Insert("INSERT INTO video_chunk(upload_id, chunk_index, chunk_size, chunk_md5, file_id, status) " +
            "VALUES(#{uploadId}, #{chunkIndex}, #{chunkSize}, #{chunkMd5}, #{fileId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VideoChunk chunk);

    /** 根据上传会话ID和分片序号查询（判断分片是否已上传） */
    @Select("SELECT * FROM video_chunk WHERE upload_id = #{uploadId} AND chunk_index = #{chunkIndex}")
    VideoChunk selectByUploadIdAndIndex(@Param("uploadId") String uploadId, @Param("chunkIndex") Integer chunkIndex);

    /** 查询某上传会话已上传的分片序号列表（用于断点续传返回已传分片） */
    @Select("SELECT chunk_index FROM video_chunk WHERE upload_id = #{uploadId} AND status = 0 ORDER BY chunk_index")
    List<Integer> selectUploadedIndexes(@Param("uploadId") String uploadId);

    /** 查询某上传会话的所有分片（按序号排序，用于合并） */
    @Select("SELECT * FROM video_chunk WHERE upload_id = #{uploadId} AND status = 0 ORDER BY chunk_index")
    List<VideoChunk> selectByUploadId(@Param("uploadId") String uploadId);

    /** 将分片标记为已合并并关联视频ID */
    @Update("UPDATE video_chunk SET status = 1, video_id = #{videoId} WHERE upload_id = #{uploadId} AND status = 0")
    int markMerged(@Param("uploadId") String uploadId, @Param("videoId") Long videoId);

    /** 删除某上传会话的所有分片记录 */
    @Delete("DELETE FROM video_chunk WHERE upload_id = #{uploadId}")
    int deleteByUploadId(@Param("uploadId") String uploadId);

    /** 删除某上传会话中已完成（已合并/已清理）的分片记录，避免同文件重新上传时被当成"已上传"而数量对不上 */
    @Delete("DELETE FROM video_chunk WHERE upload_id = #{uploadId} AND status != 0")
    int deleteNonUploaded(@Param("uploadId") String uploadId);
}
