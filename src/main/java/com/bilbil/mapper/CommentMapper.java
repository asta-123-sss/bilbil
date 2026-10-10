package com.bilbil.mapper;

import com.bilbil.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论表 Mapper（支持子评论/回复）
 */
@Mapper
public interface CommentMapper {

    /** 新增评论，返回自增主键 */
    @Insert("INSERT INTO comment(video_id, user_id, parent_id, root_id, reply_user_id, content, status) " +
            "VALUES(#{videoId}, #{userId}, #{parentId}, #{rootId}, #{replyUserId}, #{content}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /** 根据ID查询 */
    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment selectById(Long id);

    /** 分页查询一级评论（parent_id = 0） */
    @Select("SELECT * FROM comment WHERE video_id = #{videoId} AND parent_id = 0 AND status = 0 " +
            "ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<Comment> selectTopLevel(@Param("videoId") Long videoId, @Param("offset") int offset, @Param("size") int size);

    /** 统计一级评论数 */
    @Select("SELECT COUNT(*) FROM comment WHERE video_id = #{videoId} AND parent_id = 0 AND status = 0")
    long countTopLevel(@Param("videoId") Long videoId);

    /** 分页查询某根评论下的所有回复（含二级回复） */
    @Select("SELECT * FROM comment WHERE video_id = #{videoId} AND root_id = #{rootId} AND status = 0 " +
            "ORDER BY id ASC LIMIT #{offset}, #{size}")
    List<Comment> selectReplies(@Param("videoId") Long videoId, @Param("rootId") Long rootId,
                                @Param("offset") int offset, @Param("size") int size);

    /** 统计某根评论的回复数 */
    @Select("SELECT COUNT(*) FROM comment WHERE video_id = #{videoId} AND root_id = #{rootId} AND status = 0")
    long countReplies(@Param("videoId") Long videoId, @Param("rootId") Long rootId);

    /** 软删除评论 */
    @Update("UPDATE comment SET status = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    /** 后台：按内容关键字分页查询未删除评论 */
    @Select("<script>SELECT * FROM comment " +
            "<where>" +
            "  status = 0" +
            "  <if test='keyword != null and keyword != \"\"'>AND content LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{size}</script>")
    List<Comment> selectAdminList(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    /** 后台：按内容关键字统计未删除评论总数 */
    @Select("<script>SELECT COUNT(*) FROM comment " +
            "<where>" +
            "  status = 0" +
            "  <if test='keyword != null and keyword != \"\"'>AND content LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where></script>")
    long countAdmin(@Param("keyword") String keyword);
}
