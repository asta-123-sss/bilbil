package com.bilbil.mapper;

import com.bilbil.entity.Danmaku;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 弹幕表 Mapper
 */
@Mapper
public interface DanmakuMapper {

    /** 新增弹幕 */
    @Insert("INSERT INTO danmaku(video_id, user_id, content, video_time, color, mode, font_size, status) " +
            "VALUES(#{videoId}, #{userId}, #{content}, #{videoTime}, #{color}, #{mode}, #{fontSize}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Danmaku danmaku);

    /** 按时间区间查询弹幕（B站按视频进度分段加载弹幕） */
    @Select("SELECT * FROM danmaku WHERE video_id = #{videoId} AND status = 0 " +
            "AND video_time BETWEEN #{from} AND #{to} ORDER BY video_time ASC")
    List<Danmaku> selectByTimeRange(@Param("videoId") Long videoId,
                                    @Param("from") int from,
                                    @Param("to") int to);
}
