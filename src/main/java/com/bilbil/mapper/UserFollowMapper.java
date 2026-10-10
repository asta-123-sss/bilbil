package com.bilbil.mapper;

import com.bilbil.entity.User;
import com.bilbil.entity.UserFollow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户关注表 Mapper
 */
@Mapper
public interface UserFollowMapper {

    /** 新增关注关系 */
    @Insert("INSERT INTO user_follow(user_id, follow_id) VALUES(#{userId}, #{followId})")
    int insert(UserFollow follow);

    /** 删除关注关系（取关） */
    @Delete("DELETE FROM user_follow WHERE user_id = #{userId} AND follow_id = #{followId}")
    int delete(@Param("userId") Long userId, @Param("followId") Long followId);

    /** 查询关注关系是否存在 */
    @Select("SELECT * FROM user_follow WHERE user_id = #{userId} AND follow_id = #{followId}")
    UserFollow selectByUserIdAndFollowId(@Param("userId") Long userId, @Param("followId") Long followId);

    /** 关注数（我关注了多少人） */
    @Select("SELECT COUNT(*) FROM user_follow WHERE user_id = #{userId}")
    long countFollowings(@Param("userId") Long userId);

    /** 粉丝数（多少人关注我） */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follow_id = #{userId}")
    long countFans(@Param("userId") Long userId);

    /** 查询某用户的关注ID列表（用于 Redis 缓存预热） */
    @Select("SELECT follow_id FROM user_follow WHERE user_id = #{userId}")
    List<Long> selectFollowIds(@Param("userId") Long userId);

    /** 查询某用户的粉丝ID列表（用于 Redis 缓存预热） */
    @Select("SELECT user_id FROM user_follow WHERE follow_id = #{userId}")
    List<Long> selectFanIds(@Param("userId") Long userId);

    /** 分页查询关注列表（联表取用户信息） */
    @Select("SELECT u.* FROM user_follow uf JOIN `user` u ON uf.follow_id = u.id " +
            "WHERE uf.user_id = #{userId} ORDER BY uf.create_time DESC LIMIT #{offset}, #{size}")
    List<User> selectFollowings(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    /** 分页查询粉丝列表（联表取用户信息） */
    @Select("SELECT u.* FROM user_follow uf JOIN `user` u ON uf.user_id = u.id " +
            "WHERE uf.follow_id = #{userId} ORDER BY uf.create_time DESC LIMIT #{offset}, #{size}")
    List<User> selectFans(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
}
