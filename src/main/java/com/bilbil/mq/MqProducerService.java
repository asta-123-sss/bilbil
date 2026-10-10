package com.bilbil.mq;

import com.bilbil.config.BilbilProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 生产者封装
 * <p>业务层统一通过该类发送异步消息。当 MQ 未启用时（producer 为 null）仅打印日志，
 * 由调用方决定是否降级为本地直接处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducerService {

    private final BilbilProperties properties;

    /** MQ 未启用时为 null（@Autowired(required=false) 注入） */
    @Autowired(required = false)
    private DefaultMQProducer producer;

    /**
     * 发送普通消息
     *
     * @param topic 主题
     * @param tag   标签
     * @param body  消息体（JSON 字符串）
     */
    public void send(String topic, String tag, String body) {
        if (!properties.getRocketmq().isEnabled() || producer == null) {
            log.info("[MQ未启用] 跳过发送 topic={}, tag={}, body={}", topic, tag, body);
            return;
        }
        try {
            Message message = new Message(topic, tag, body.getBytes(StandardCharsets.UTF_8));
            producer.send(message);
            log.info("[MQ发送成功] topic={}, tag={}", topic, tag);
        } catch (Exception e) {
            // 发送失败只记录日志，不阻断主业务流程（消息可重试/补偿）
            log.error("[MQ发送失败] topic={}, tag={}", topic, tag, e);
        }
    }

    /** 当前是否启用 RocketMQ */
    public boolean isEnabled() {
        return properties.getRocketmq().isEnabled() && producer != null;
    }
}
