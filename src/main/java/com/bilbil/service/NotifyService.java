package com.bilbil.service;

import com.bilbil.common.constant.MessageTypeConstant;
import com.bilbil.entity.MessageNotify;
import com.bilbil.mapper.MessageNotifyMapper;
import com.bilbil.mq.MqConstant;
import com.bilbil.mq.MqProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息通知服务
 * <p>点赞/评论/关注/回复时调用，通过 RocketMQ 异步发送（消费端落库），
 * 解耦主业务流程提升吞吐量；MQ 未启用时直接本地落库降级。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final MqProducerService mqProducerService;
    private final MessageNotifyMapper messageNotifyMapper;
    private final ObjectMapper objectMapper;

    /**
     * 点赞通知
     *
     * @param receiverId 接收者（被点赞的视频作者）
     * @param senderId   触发者（点赞的人）
     * @param videoId    被点赞的视频
     */
    public void notifyLike(Long receiverId, Long senderId, Long videoId) {
        // 自己给自己点赞不发通知
        if (receiverId.equals(senderId)) {
            return;
        }
        send(build(receiverId, senderId, MessageTypeConstant.LIKE, "点赞了你的视频", videoId),
                MqConstant.TAG_LIKE);
    }

    /**
     * 评论通知
     */
    public void notifyComment(Long receiverId, Long senderId, Long videoId, String content) {
        if (receiverId.equals(senderId)) {
            return;
        }
        String shortContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        send(build(receiverId, senderId, MessageTypeConstant.COMMENT, "评论了你的视频：" + shortContent, videoId),
                MqConstant.TAG_COMMENT);
    }

    /**
     * 回复通知（回复某条评论）
     */
    public void notifyReply(Long receiverId, Long senderId, Long videoId, String content) {
        if (receiverId.equals(senderId)) {
            return;
        }
        String shortContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        send(build(receiverId, senderId, MessageTypeConstant.REPLY, "回复了你：" + shortContent, videoId),
                MqConstant.TAG_REPLY);
    }

    /**
     * 关注通知
     */
    public void notifyFollow(Long receiverId, Long senderId) {
        if (receiverId.equals(senderId)) {
            return;
        }
        send(build(receiverId, senderId, MessageTypeConstant.FOLLOW, "关注了你", null),
                MqConstant.TAG_FOLLOW);
    }

    /** 构建消息对象 */
    private MessageNotify build(Long receiverId, Long senderId, int type, String content, Long videoId) {
        return MessageNotify.builder()
                .userId(receiverId)
                .senderId(senderId)
                .type(type)
                .content(content)
                .videoId(videoId)
                .isRead(0)
                .build();
    }

    /** 发送：MQ 启用则发消息，否则本地直接落库 */
    private void send(MessageNotify notify, String tag) {
        if (mqProducerService.isEnabled()) {
            try {
                mqProducerService.send(MqConstant.TOPIC_NOTIFY, tag, objectMapper.writeValueAsString(notify));
            } catch (Exception e) {
                log.error("通知消息序列化失败", e);
            }
        } else {
            messageNotifyMapper.insert(notify);
        }
    }
}
