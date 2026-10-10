package com.bilbil.mapper;

import com.bilbil.entity.PrivateMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 私信表 Mapper
 */
@Mapper
public interface PrivateMessageMapper {

    /** 新增私信 */
    @Insert("INSERT INTO private_message(from_user_id, to_user_id, content, is_read) " +
            "VALUES(#{fromUserId}, #{toUserId}, #{content}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PrivateMessage message);

    /** 查询两人最近 limit 条消息（时间正序，用于聊天窗口展示） */
    @Select("SELECT * FROM (" +
            "  SELECT * FROM private_message " +
            "  WHERE (from_user_id = #{userId} AND to_user_id = #{otherId}) " +
            "     OR (from_user_id = #{otherId} AND to_user_id = #{userId}) " +
            "  ORDER BY id DESC LIMIT #{limit}" +
            ") t ORDER BY id ASC")
    List<PrivateMessage> selectChatAsc(@Param("userId") Long userId,
                                       @Param("otherId") Long otherId,
                                       @Param("limit") int limit);

    /** 查询用户参与的所有会话（按对方分组，返回对方ID与最后消息时间） */
    @Select("SELECT CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END AS otherId, " +
            "MAX(create_time) AS lastTime " +
            "FROM private_message " +
            "WHERE from_user_id = #{userId} OR to_user_id = #{userId} " +
            "GROUP BY otherId ORDER BY lastTime DESC")
    List<Map<String, Object>> selectConversationPeers(@Param("userId") Long userId);

    /** 查询两人最后一条消息 */
    @Select("SELECT * FROM private_message " +
            "WHERE (from_user_id = #{userId} AND to_user_id = #{otherId}) " +
            "   OR (from_user_id = #{otherId} AND to_user_id = #{userId}) " +
            "ORDER BY id DESC LIMIT 1")
    PrivateMessage selectLastMessage(@Param("userId") Long userId, @Param("otherId") Long otherId);

    /** 查询某会话中「对方发给我」的未读数 */
    @Select("SELECT COUNT(*) FROM private_message " +
            "WHERE from_user_id = #{otherId} AND to_user_id = #{userId} AND is_read = 0")
    long countUnreadBetween(@Param("userId") Long userId, @Param("otherId") Long otherId);

    /** 查询用户全部未读私信数 */
    @Select("SELECT COUNT(*) FROM private_message WHERE to_user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    /** 将某会话中「对方发给我」的消息标记为已读 */
    @Update("UPDATE private_message SET is_read = 1 " +
            "WHERE from_user_id = #{otherId} AND to_user_id = #{userId} AND is_read = 0")
    int markReadBetween(@Param("userId") Long userId, @Param("otherId") Long otherId);
}
