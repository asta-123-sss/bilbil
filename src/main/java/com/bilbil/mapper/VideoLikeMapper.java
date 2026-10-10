package com.bilbil.mapper;

import com.bilbil.entity.VideoLike;
import org.apache.ibatis.annotations.Insert;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 视频点赞表 Mapper
 * <p>点赞数不走本表 COUNT，而是 Redis 集合原子维护，本表仅记录用户-视频点赞关系用于回查</p>
 */
@Mapper
public interface VideoLikeMapper {

    /** 新增点赞关系 */
    @Insert("INSERT INTO video_like(user_id, video_id, status) VALUES(#{userId}, #{videoId}, #{status}) " +
            "ON DUPLICATE KEY UPDATE status = VALUES(status)")
    int insert(VideoLike like);

    /** 更新点赞状态（点赞/取消） */
    @Update("UPDATE video_like SET status = #{status} WHERE user_id = #{userId} AND video_id = #{videoId}")
    int updateStatus(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("status") Integer status);

    /** 查询点赞关系 */
    @Select("SELECT * FROM video_like WHERE user_id = #{userId} AND video_id = #{videoId}")
    VideoLike selectByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 查询某视频所有已点赞用户ID（用于 Redis 集合回填） */
    @Select("SELECT user_id FROM video_like WHERE video_id = #{videoId} AND status = 1")
    List<Long> selectUserIdsByVideoId(@Param("videoId") Long videoId);
}
