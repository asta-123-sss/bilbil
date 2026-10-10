package com.bilbil.mq;

import com.bilbil.common.constant.VideoConstant;
import com.bilbil.config.BilbilProperties;
import com.bilbil.mapper.VideoMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
 * 视频转码消费者
 * <p>消费 bilbil-video-topic 的 transcode 消息。上传合并完成后发送转码消息，
 * 本消费者模拟转码处理（真实项目接入 FFmpeg / 转码集群），完成后将视频状态更新为已发布。
 * 转码是耗时任务，通过 MQ 异步化避免阻塞上传接口。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bilbil.rocketmq", name = "enabled", havingValue = "true")
public class VideoTranscodeConsumer {

    private final VideoMapper videoMapper;
    private final ObjectMapper objectMapper;
    private final BilbilProperties properties;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void start() throws Exception {
        BilbilProperties.RocketMq cfg = properties.getRocketmq();
        // 视频任务使用独立消费组
        consumer = new DefaultMQPushConsumer(cfg.getConsumerGroup() + "-video");
        consumer.setNamesrvAddr(cfg.getNameServer());
        consumer.subscribe(MqConstant.TOPIC_VIDEO, MqConstant.TAG_TRANSCODE);
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                try {
                    String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                    JsonNode node = objectMapper.readTree(body);
                    Long videoId = node.get("videoId").asLong();

                    log.info("开始转码视频：videoId={}", videoId);
                    // 模拟转码耗时（真实场景调用 FFmpeg 生成多清晰度、抽帧封面等）
                    Thread.sleep(1000);

                    // 转码完成，更新视频状态为已发布
                    videoMapper.updateStatus(videoId, VideoConstant.Status.PUBLISHED);
                    log.info("视频转码完成并发布：videoId={}", videoId);
                } catch (Exception e) {
                    log.error("视频转码消费失败，稍后重试", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("视频转码消费者启动，订阅主题={}, tag={}", MqConstant.TOPIC_VIDEO, MqConstant.TAG_TRANSCODE);
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
