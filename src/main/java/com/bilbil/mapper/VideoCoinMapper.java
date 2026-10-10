package com.bilbil.mapper;

import com.bilbil.entity.VideoCoin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 视频投币表 Mapper
 * <p>投币数由 Redis 集合原子维护，本表记录用户-视频投币关系</p>
 */
@Mapper
public interface VideoCoinMapper {

    /** 新增投币关系 */
    @Insert("INSERT INTO video_coin(user_id, video_id, status) VALUES(#{userId}, #{videoId}, #{status}) " +
            "ON DUPLICATE KEY UPDATE status = VALUES(status)")
    int insert(VideoCoin coin);

    /** 更新投币状态（投币/取消） */
    @Update("UPDATE video_coin SET status = #{status} WHERE user_id = #{userId} AND video_id = #{videoId}")
    int updateStatus(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("status") Integer status);

    /** 查询投币关系 */
    @Select("SELECT * FROM video_coin WHERE user_id = #{userId} AND video_id = #{videoId}")
    VideoCoin selectByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 查询某视频所有已投币用户ID（用于 Redis 集合回填） */
    @Select("SELECT user_id FROM video_coin WHERE video_id = #{videoId} AND status = 1")
    List<Long> selectUserIdsByVideoId(@Param("videoId") Long videoId);
}
