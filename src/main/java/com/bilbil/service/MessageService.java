package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.entity.MessageNotify;
import com.bilbil.entity.User;
import com.bilbil.mapper.MessageNotifyMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.vo.MessageNotifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息通知查询服务（消息由 NotifyService 生产、MQ 消费端/降级落库）
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageNotifyMapper messageNotifyMapper;
    private final UserMapper userMapper;

    /**
     * 分页查询用户消息
     */
    public PageResult<MessageNotifyVO> list(Long userId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        long total = messageNotifyMapper.countByUserId(userId);
        List<MessageNotify> messages = messageNotifyMapper.selectByUserId(userId, (p - 1) * s, s);

        // 批量加载发送者信息
        List<Long> senderIds = messages.stream().map(MessageNotify::getSenderId).distinct().toList();
        Map<Long, User> userMap = senderIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(senderIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<MessageNotifyVO> list = messages.stream().map(m -> {
            User sender = userMap.get(m.getSenderId());
            return MessageNotifyVO.builder()
                    .id(m.getId())
                    .type(m.getType())
                    .content(m.getContent())
                    .videoId(m.getVideoId())
                    .isRead(m.getIsRead())
                    .senderId(m.getSenderId())
                    .senderName(sender == null ? null : StrUtil.blankToDefault(sender.getNickname(), sender.getUsername()))
                    .senderAvatar(sender == null ? null : sender.getAvatar())
                    .createTime(m.getCreateTime())
                    .build();
        }).toList();
        return PageResult.of(total, p, s, list);
    }

    /**
     * 未读消息数
     */
    public long unreadCount(Long userId) {
        return messageNotifyMapper.countUnread(userId);
    }

    /**
     * 标记单条消息已读
     */
    public void markRead(Long userId, Long messageId) {
        int rows = messageNotifyMapper.markRead(messageId, userId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
    }

    /**
     * 标记全部已读
     */
    public void markAllRead(Long userId) {
        messageNotifyMapper.markAllRead(userId);
    }
}
