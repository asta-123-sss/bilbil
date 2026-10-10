package com.bilbil.mapper;

import com.bilbil.entity.Ad;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 广告表 Mapper
 */
@Mapper
public interface AdMapper {

    /** 新增广告，状态默认 1（投放中） */
    @Insert("INSERT INTO ad(user_id, title, cover_url, target_url, category_id, cpm, budget, impressions, clicks, spent, status) " +
            "VALUES(#{userId}, #{title}, #{coverUrl}, #{targetUrl}, #{categoryId}, #{cpm}, #{budget}, 0, 0, 0, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ad ad);

    /** 根据ID查询 */
    @Select("SELECT * FROM ad WHERE id = #{id}")
    Ad selectById(Long id);

    /** 查询某商家广告（分页） */
    @Select("SELECT * FROM ad WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<Ad> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    /** 某商家广告总数 */
    @Select("SELECT COUNT(*) FROM ad WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /** 更新广告状态 */
    @Update("UPDATE ad SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 曝光 +1 */
    @Update("UPDATE ad SET impressions = impressions + 1 WHERE id = #{id}")
    int incrementImpression(Long id);

    /** 点击 +1 */
    @Update("UPDATE ad SET clicks = clicks + 1 WHERE id = #{id}")
    int incrementClick(Long id);

    /** 累计消耗积分 */
    @Update("UPDATE ad SET spent = spent + #{delta} WHERE id = #{id}")
    int incrementSpent(@Param("id") Long id, @Param("delta") Long delta);

    /** 删除广告 */
    @Delete("DELETE FROM ad WHERE id = #{id}")
    int deleteById(Long id);

    /** 取投放中的广告用于投放（兴趣定向：可选按分类过滤），按 id 倒序取前 limit 条 */
    @Select("<script>SELECT * FROM ad WHERE status = 1 AND spent &lt; budget " +
            "<if test='categoryIds != null and categoryIds.size() > 0'>" +
            "AND category_id IN <foreach collection='categoryIds' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</if>" +
            "ORDER BY RAND() LIMIT #{limit}</script>")
    List<Ad> selectActiveForServing(@Param("categoryIds") List<Long> categoryIds, @Param("limit") int limit);

    /** 条件分页查询（后台管理） */
    @Select("<script>SELECT * FROM ad " +
            "<where>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{size}</script>")
    List<Ad> selectList(@Param("status") Integer status, @Param("keyword") String keyword,
                        @Param("offset") int offset, @Param("size") int size);

    /** 条件查询总数（与 selectList 保持一致） */
    @Select("<script>SELECT COUNT(*) FROM ad " +
            "<where>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</where></script>")
    long count(@Param("status") Integer status, @Param("keyword") String keyword);
}
