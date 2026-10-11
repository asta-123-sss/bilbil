package com.bilbil.mapper;

import com.bilbil.entity.WatchHistory;
import com.bilbil.vo.WatchHistoryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 观看历史表 Mapper
 * <p>同一用户+视频只保留一条记录，重复观看刷新进度与时间。</p>
 */
@Mapper
public interface WatchHistoryMapper {

    /** 新增/更新观看记录（存在则刷新进度与观看时间） */
    @Insert("INSERT INTO watch_history(user_id, video_id, progress) VALUES(#{userId}, #{videoId}, #{progress}) " +
            "ON DUPLICATE KEY UPDATE progress = VALUES(progress), update_time = NOW()")
    int upsert(WatchHistory history);

    /** 分页查询观看历史（联表取视频标题/封面/时长/作者/播放量，按最近观看倒序） */
    @Select("SELECT w.video_id AS videoId, w.progress AS progress, w.update_time AS watchTime, " +
            "v.title AS title, v.cover_url AS coverUrl, v.duration AS duration, " +
            "v.user_id AS authorId, v.play_count AS playCount " +
            "FROM watch_history w JOIN video v ON v.id = w.video_id " +
            "WHERE w.user_id = #{userId} ORDER BY w.update_time DESC LIMIT #{offset}, #{size}")
    List<WatchHistoryVO> selectByUserId(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    /** 观看历史总数 */
    @Select("SELECT COUNT(*) FROM watch_history WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    /** 删除单条历史 */
    @Delete("DELETE FROM watch_history WHERE user_id = #{userId} AND video_id = #{videoId}")
    int deleteByUserAndVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 清空全部历史 */
    @Delete("DELETE FROM watch_history WHERE user_id = #{userId}")
    int clearAll(@Param("userId") Long userId);

    /** 查询用户观看过的所有视频ID（个性化推荐去重/排除用） */
    @Select("SELECT video_id FROM watch_history WHERE user_id = #{userId}")
    List<Long> selectVideoIdsByUserId(@Param("userId") Long userId);

    /** 查询用户最近观看过的视频标题（AI 智能广告投放的兴趣信号） */
    @Select("SELECT v.title FROM watch_history w JOIN video v ON v.id = w.video_id " +
            "WHERE w.user_id = #{userId} ORDER BY w.update_time DESC LIMIT #{limit}")
    List<String> selectRecentTitles(@Param("userId") Long userId, @Param("limit") int limit);
}
