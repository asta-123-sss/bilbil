package com.bilbil.mapper;

import com.bilbil.entity.VideoCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 视频分区表 Mapper
 */
@Mapper
public interface VideoCategoryMapper {

    /** 查询所有分区（按排序权重升序） */
    @Select("SELECT * FROM video_category ORDER BY sort ASC, id ASC")
    List<VideoCategory> selectAll();

    /** 根据ID查询 */
    @Select("SELECT * FROM video_category WHERE id = #{id}")
    VideoCategory selectById(Long id);
}
