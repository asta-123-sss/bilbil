package com.bilbil.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置
 * <p>仅当 bilbil.rocketmq.enabled=true 时创建生产者 Bean；
 * 关闭时（默认）消息由业务层直接本地落库，保证无 broker 环境也能开发调试。</p>
 */
@Configuration
public class RocketMQConfig {

    /**
     * 创建并启动 RocketMQ 生产者
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "bilbil.rocketmq", name = "enabled", havingValue = "true")
    public DefaultMQProducer defaultMQProducer(BilbilProperties properties) throws Exception {
        BilbilProperties.RocketMq cfg = properties.getRocketmq();
        DefaultMQProducer producer = new DefaultMQProducer(cfg.getProducerGroup());
        producer.setNamesrvAddr(cfg.getNameServer());
        producer.setVipChannelEnabled(false);
        producer.start();
        return producer;
    }
}
