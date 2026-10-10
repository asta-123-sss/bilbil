package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.dto.SendPmDTO;
import com.bilbil.entity.PrivateMessage;
import com.bilbil.entity.User;
import com.bilbil.mapper.PrivateMessageMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.vo.ConversationVO;
import com.bilbil.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 私信服务（点对点聊天）
 */
@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private static final int CHAT_LIMIT = 200;

    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;

    /**
     * 发送私信
     */
    public PrivateMessageVO send(Long fromId, SendPmDTO dto) {
        Long toId = dto.getToUserId();
        if (toId.equals(fromId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发私信");
        }
        User target = userMapper.selectById(toId);
        if (target == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        privateMessageMapper.insert(PrivateMessage.builder()
                .fromUserId(fromId).toUserId(toId).content(dto.getContent()).isRead(0).build());
        // 回读最后一条（取 DB 生成的 id 与时间）
        return toVO(privateMessageMapper.selectLastMessage(fromId, toId));
    }

    /**
     * 会话列表（按最后消息时间倒序）
     */
    public List<ConversationVO> conversations(Long userId) {
        List<Map<String, Object>> peers = privateMessageMapper.selectConversationPeers(userId);
        if (peers.isEmpty()) {
            return List.of();
        }
        List<Long> otherIds = peers.stream()
                .map(m -> toLong(m.get("otherId")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> userMap = otherIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(otherIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return peers.stream().map(m -> {
            Long otherId = toLong(m.get("otherId"));
            User other = userMap.get(otherId);
            PrivateMessage last = privateMessageMapper.selectLastMessage(userId, otherId);
            return ConversationVO.builder()
                    .otherUserId(otherId)
                    .otherUserName(other == null ? null : StrUtil.blankToDefault(other.getNickname(), other.getUsername()))
                    .otherUserAvatar(other == null ? null : other.getAvatar())
                    .lastContent(last == null ? null : last.getContent())
                    .lastTime(last == null ? null : last.getCreateTime())
                    .unreadCount(privateMessageMapper.countUnreadBetween(userId, otherId))
                    .build();
        }).toList();
    }

    /**
     * 与某人的聊天记录（时间正序），并把「对方发给我」的消息标记为已读
     */
    public List<PrivateMessageVO> chat(Long userId, Long otherId) {
        List<PrivateMessage> messages = privateMessageMapper.selectChatAsc(userId, otherId, CHAT_LIMIT);
        privateMessageMapper.markReadBetween(userId, otherId);
        return messages.stream().map(this::toVO).toList();
    }

    /**
     * 未读私信总数
     */
    public long unreadCount(Long userId) {
        return privateMessageMapper.countUnread(userId);
    }

    /**
     * 将某会话标记为已读
     */
    public void markRead(Long userId, Long otherId) {
        privateMessageMapper.markReadBetween(userId, otherId);
    }

    private PrivateMessageVO toVO(PrivateMessage m) {
        return PrivateMessageVO.builder()
                .id(m.getId())
                .fromUserId(m.getFromUserId())
                .toUserId(m.getToUserId())
                .content(m.getContent())
                .isRead(m.getIsRead())
                .createTime(m.getCreateTime())
                .build();
    }

    /** 兼容 Number / String / Timestamp 的取值 */
    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(v.toString());
    }
}
