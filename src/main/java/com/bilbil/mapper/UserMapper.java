package com.bilbil.mapper;

import com.bilbil.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户表 Mapper
 */
@Mapper
public interface UserMapper {

    /** 新增用户，返回自增主键 */
    @Insert("INSERT INTO `user`(username, password, nickname, avatar, gender, birthday, signature, email, phone, status) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{avatar}, #{gender}, #{birthday}, #{signature}, #{email}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /** 根据ID查询 */
    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User selectById(Long id);

    /** 根据用户名查询 */
    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectByUsername(String username);

    /** 批量按ID查询（用于列表页展示作者信息） */
    @Select("<script>SELECT * FROM `user` WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<User> selectByIds(@Param("ids") List<Long> ids);

    /** 动态更新用户信息（只更新非空字段，password 单独修改） */
    @Update("<script>UPDATE `user` <set>" +
            "<if test='nickname != null'>nickname = #{nickname},</if>" +
            "<if test='avatar != null'>avatar = #{avatar},</if>" +
            "<if test='gender != null'>gender = #{gender},</if>" +
            "<if test='birthday != null'>birthday = #{birthday},</if>" +
            "<if test='signature != null'>signature = #{signature},</if>" +
            "<if test='email != null'>email = #{email},</if>" +
            "<if test='phone != null'>phone = #{phone},</if>" +
            "</set> WHERE id = #{id}</script>")
    int update(User user);

    /** 修改密码 */
    @Update("UPDATE `user` SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 更新账号状态（封禁/解封） */
    @Update("UPDATE `user` SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 更新用户角色（商家审核通过时设为 2） */
    @Update("UPDATE `user` SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") Integer role);

    /** 积分余额增减（充值 / 广告扣费） */
    @Update("UPDATE `user` SET balance = balance + #{delta} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("delta") Long delta);

    /** 分页查询商家（role = 2），后台管理用 */
    @Select("<script>SELECT * FROM `user` WHERE role = 2 " +
            "<if test='keyword != null and keyword != \"\"'>AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "ORDER BY id DESC LIMIT #{offset}, #{size}</script>")
    List<User> selectMerchants(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    /** 商家总数 */
    @Select("<script>SELECT COUNT(*) FROM `user` WHERE role = 2 " +
            "<if test='keyword != null and keyword != \"\"'>AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))</if></script>")
    long countMerchants(@Param("keyword") String keyword);

    /** 条件分页查询用户（后台管理，支持 keyword 模糊匹配用户名/昵称） */
    @Select("<script>SELECT * FROM `user` " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{size}</script>")
    List<User> selectList(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    /** 条件查询用户总数（与 selectList 保持一致） */
    @Select("<script>SELECT COUNT(*) FROM `user` " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where></script>")
    long count(@Param("keyword") String keyword);
}
