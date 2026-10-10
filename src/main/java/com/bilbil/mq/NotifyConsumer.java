package com.bilbil.mq;

import com.bilbil.config.BilbilProperties;
import com.bilbil.entity.MessageNotify;
import com.bilbil.mapper.MessageNotifyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 消息通知消费者
 * <p>消费 bilbil-notify-topic，将点赞/评论/关注/回复通知写入 message_notify 表。
 * 通过 MQ 解耦：互动接口只发消息不落库，提升接口吞吐量。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bilbil.rocketmq", name = "enabled", havingValue = "true")
public class NotifyConsumer {

    private final MessageNotifyMapper messageNotifyMapper;
    private final ObjectMapper objectMapper;
    private final BilbilProperties properties;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void start() throws Exception {
        BilbilProperties.RocketMq cfg = properties.getRocketmq();
        // 每个消费组订阅关系必须一致，故通知消费者使用独立消费组
        consumer = new DefaultMQPushConsumer(cfg.getConsumerGroup() + "-notify");
        consumer.setNamesrvAddr(cfg.getNameServer());
        consumer.subscribe(MqConstant.TOPIC_NOTIFY, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                try {
                    String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                    MessageNotify notify = objectMapper.readValue(body, MessageNotify.class);
                    messageNotifyMapper.insert(notify);
                } catch (Exception e) {
                    log.error("通知消息消费失败，稍后重试", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("消息通知消费者启动，订阅主题={}", MqConstant.TOPIC_NOTIFY);
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
