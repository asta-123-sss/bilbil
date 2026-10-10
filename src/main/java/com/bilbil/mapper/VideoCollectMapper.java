package com.bilbil.mapper;

import com.bilbil.entity.VideoCollect;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 视频收藏表 Mapper
 * <p>收藏数由 Redis 集合原子维护，本表记录用户-视频收藏关系</p>
 */
@Mapper
public interface VideoCollectMapper {

    /** 新增收藏关系 */
    @Insert("INSERT INTO video_collect(user_id, video_id, status) VALUES(#{userId}, #{videoId}, #{status}) " +
            "ON DUPLICATE KEY UPDATE status = VALUES(status)")
    int insert(VideoCollect collect);

    /** 更新收藏状态 */
    @Update("UPDATE video_collect SET status = #{status} WHERE user_id = #{userId} AND video_id = #{videoId}")
    int updateStatus(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("status") Integer status);

    /** 查询收藏关系 */
    @Select("SELECT * FROM video_collect WHERE user_id = #{userId} AND video_id = #{videoId}")
    VideoCollect selectByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 查询用户收藏的视频ID列表（分页） */
    @Select("SELECT video_id FROM video_collect WHERE user_id = #{userId} AND status = 1 ORDER BY update_time DESC LIMIT #{offset}, #{size}")
    List<Long> selectCollectedVideoIds(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    /** 查询用户收藏总数 */
    @Select("SELECT COUNT(*) FROM video_collect WHERE user_id = #{userId} AND status = 1")
    long countByUserId(@Param("userId") Long userId);

    /** 查询某视频所有已收藏用户ID（用于 Redis 集合回填） */
    @Select("SELECT user_id FROM video_collect WHERE video_id = #{videoId} AND status = 1")
    List<Long> selectUserIdsByVideoId(@Param("videoId") Long videoId);
}
