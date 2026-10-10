package com.bilbil.mapper;

import com.bilbil.entity.MessageNotify;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息通知表 Mapper
 */
@Mapper
public interface MessageNotifyMapper {

    /** 新增消息 */
    @Insert("INSERT INTO message_notify(user_id, sender_id, type, content, video_id, is_read) " +
            "VALUES(#{userId}, #{senderId}, #{type}, #{content}, #{videoId}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MessageNotify notify);

    /** 分页查询用户消息 */
    @Select("SELECT * FROM message_notify WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<MessageNotify> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    /** 查询用户消息总数 */
    @Select("SELECT COUNT(*) FROM message_notify WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    /** 查询未读消息数 */
    @Select("SELECT COUNT(*) FROM message_notify WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    /** 标记单条消息已读 */
    @Update("UPDATE message_notify SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 标记全部消息已读 */
    @Update("UPDATE message_notify SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
