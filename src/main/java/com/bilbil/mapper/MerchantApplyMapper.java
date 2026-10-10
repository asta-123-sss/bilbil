package com.bilbil.mapper;

import com.bilbil.entity.MerchantApply;
import com.bilbil.vo.MerchantApplyVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商家申请表 Mapper
 */
@Mapper
public interface MerchantApplyMapper {

    /** 新增申请，状态默认 0（待审核） */
    @Insert("INSERT INTO merchant_apply(user_id, real_name, contact, reason, status) " +
            "VALUES(#{userId}, #{realName}, #{contact}, #{reason}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MerchantApply apply);

    /** 根据ID查询 */
    @Select("SELECT * FROM merchant_apply WHERE id = #{id}")
    MerchantApply selectById(Long id);

    /** 查询某用户最新一条申请 */
    @Select("SELECT * FROM merchant_apply WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    MerchantApply selectLatestByUserId(Long userId);

    /** 审核：更新状态、备注与审核时间 */
    @Update("UPDATE merchant_apply SET status = #{status}, review_note = #{note}, review_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("note") String note);

    /** 条件分页查询（联表取申请人账号，后台审核用） */
    @Select("<script>SELECT a.id AS id, a.user_id AS userId, u.username AS username, u.nickname AS nickname, " +
            "a.real_name AS realName, a.contact AS contact, a.reason AS reason, a.status AS status, " +
            "a.review_note AS reviewNote, a.create_time AS createTime, a.review_time AS reviewTime " +
            "FROM merchant_apply a JOIN `user` u ON u.id = a.user_id " +
            "<where>" +
            "  <if test='status != null'>AND a.status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR a.real_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "</where>" +
            "ORDER BY a.id DESC LIMIT #{offset}, #{size}</script>")
    List<MerchantApplyVO> selectList(@Param("status") Integer status, @Param("keyword") String keyword,
                                     @Param("offset") int offset, @Param("size") int size);

    /** 条件查询总数（与 selectList 保持一致） */
    @Select("<script>SELECT COUNT(*) FROM merchant_apply a JOIN `user` u ON u.id = a.user_id " +
            "<where>" +
            "  <if test='status != null'>AND a.status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR a.real_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "</where></script>")
    long count(@Param("status") Integer status, @Param("keyword") String keyword);
}
