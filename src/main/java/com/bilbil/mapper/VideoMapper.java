package com.bilbil.mapper;

import com.bilbil.entity.Video;
import com.bilbil.vo.CategoryStatVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 视频主表 Mapper
 */
@Mapper
public interface VideoMapper {

    /** 新增视频，返回自增主键 */
    @Insert("INSERT INTO video(user_id, title, description, category_id, tags, cover_url, video_url, file_id, file_md5, file_name, file_size, duration, visibility, status) " +
            "VALUES(#{userId}, #{title}, #{description}, #{categoryId}, #{tags}, #{coverUrl}, #{videoUrl}, #{fileId}, #{fileMd5}, #{fileName}, #{fileSize}, #{duration}, #{visibility}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Video video);

    /** 根据ID查询 */
    @Select("SELECT * FROM video WHERE id = #{id}")
    Video selectById(Long id);

    /** 根据文件MD5查询（秒传去重） */
    @Select("SELECT * FROM video WHERE file_md5 = #{md5} LIMIT 1")
    Video selectByFileMd5(String md5);

    /**
     * 条件分页查询视频列表
     * 参数通过 Map 传入，支持：categoryId、keyword、userId、status、visibility、orderBy、offset、size
     */
    @Select("<script>" +
            "SELECT * FROM video " +
            "<where>" +
            "  <if test='categoryId != null'>AND category_id = #{categoryId}</if>" +
            "  <if test='userId != null'>AND user_id = #{userId}</if>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where>" +
            "<choose>" +
            "  <when test='orderBy == \"hot\"'>ORDER BY play_count DESC, id DESC</when>" +
            "  <otherwise>ORDER BY id DESC</otherwise>" +
            "</choose>" +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Video> selectList(Map<String, Object> params);

    /** 条件查询总数（与 selectList 保持一致） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM video " +
            "<where>" +
            "  <if test='categoryId != null'>AND category_id = #{categoryId}</if>" +
            "  <if test='userId != null'>AND user_id = #{userId}</if>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where>" +
            "</script>")
    long count(Map<String, Object> params);

    /** 动态更新视频元数据（标题/简介/分区/标签/封面/可见性等非空字段） */
    @Update("<script>UPDATE video <set>" +
            "<if test='title != null'>title = #{title},</if>" +
            "<if test='description != null'>description = #{description},</if>" +
            "<if test='categoryId != null'>category_id = #{categoryId},</if>" +
            "<if test='tags != null'>tags = #{tags},</if>" +
            "<if test='coverUrl != null'>cover_url = #{coverUrl},</if>" +
            "<if test='visibility != null'>visibility = #{visibility},</if>" +
            "</set> WHERE id = #{id}</script>")
    int update(Video video);

    /** 更新视频状态 */
    @Update("UPDATE video SET status = #{status}, publish_time = CASE WHEN #{status} = 1 THEN NOW() ELSE publish_time END WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 批量回写计数（Redis 缓存 -> MySQL，异步/定时落库） */
    @Update("UPDATE video SET play_count = #{playCount}, like_count = #{likeCount}, collect_count = #{collectCount}, coin_count = #{coinCount} WHERE id = #{id}")
    int updateCounts(@Param("id") Long id,
                     @Param("playCount") Long playCount,
                     @Param("likeCount") Long likeCount,
                     @Param("collectCount") Long collectCount,
                     @Param("coinCount") Long coinCount);

    /** 评论数自增 */
    @Update("UPDATE video SET comment_count = comment_count + 1 WHERE id = #{id}")
    int increaseCommentCount(@Param("id") Long id);

    /** 评论数自减 */
    @Update("UPDATE video SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{id}")
    int decreaseCommentCount(@Param("id") Long id);

    /** 弹幕数自增 */
    @Update("UPDATE video SET danmaku_count = danmaku_count + 1 WHERE id = #{id}")
    int increaseDanmakuCount(@Param("id") Long id);

    /** 删除视频 */
    @Update("DELETE FROM video WHERE id = #{id}")
    int deleteById(Long id);

    /** 查询所有视频ID（用于布隆过滤器预热） */
    @Select("SELECT id FROM video")
    List<Long> selectAllIds();

    /**
     * 用户分类偏好统计（个性化推荐用）：把 5 类行为 join video 得到 category_id，
     * 加权汇总（观看 3 / 点赞 5 / 收藏 4 / 投币 4 / 关注作者视频 2），按分倒序。
     */
    @Select("SELECT t.category_id AS categoryId, SUM(t.weight) AS score FROM (" +
            "SELECT v.category_id, 3 AS weight FROM watch_history h JOIN video v ON v.id = h.video_id WHERE h.user_id = #{userId} " +
            "UNION ALL " +
            "SELECT v.category_id, 5 FROM video_like l JOIN video v ON v.id = l.video_id WHERE l.user_id = #{userId} AND l.status = 1 " +
            "UNION ALL " +
            "SELECT v.category_id, 4 FROM video_collect c JOIN video v ON v.id = c.video_id WHERE c.user_id = #{userId} AND c.status = 1 " +
            "UNION ALL " +
            "SELECT v.category_id, 4 FROM video_coin o JOIN video v ON v.id = o.video_id WHERE o.user_id = #{userId} AND o.status = 1 " +
            "UNION ALL " +
            "SELECT v.category_id, 2 FROM user_follow f JOIN video v ON v.user_id = f.follow_id WHERE f.user_id = #{userId}" +
            ") t GROUP BY t.category_id ORDER BY score DESC")
    List<CategoryStatVO> selectUserCategoryStats(@Param("userId") Long userId);

    /** 推荐候选：指定分类下、公开已发布、非本人、排除已看的视频，按热度倒序分页 */
    @Select("<script>" +
            "SELECT * FROM video " +
            "WHERE status = 1 AND visibility = 0 " +
            "AND category_id IN <foreach collection='categoryIds' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "  AND id NOT IN <foreach collection='excludeIds' item='e' open='(' separator=',' close=')'>#{e}</foreach>" +
            "</if>" +
            "AND user_id != #{excludeUserId} " +
            "ORDER BY play_count DESC, id DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Video> selectRecommendCandidates(Map<String, Object> params);

    /** 推荐候选总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM video " +
            "WHERE status = 1 AND visibility = 0 " +
            "AND category_id IN <foreach collection='categoryIds' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "  AND id NOT IN <foreach collection='excludeIds' item='e' open='(' separator=',' close=')'>#{e}</foreach>" +
            "</if>" +
            "AND user_id != #{excludeUserId}" +
            "</script>")
    long countRecommendCandidates(Map<String, Object> params);
}
