package com.bilbil.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义业务配置（前缀 bilbil）
 * <p>统一读取 application.yml 中 bilbil.* 配置，见 application.yml</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bilbil")
public class BilbilProperties {

    /** 文件存储配置 */
    private Storage storage = new Storage();
    /** FastDFS 配置 */
    private FastDfs fastdfs = new FastDfs();
    /** RocketMQ 配置 */
    private RocketMq rocketmq = new RocketMq();
    /** token 会话配置 */
    private Token token = new Token();
    /** AI 内容审核配置 */
    private Moderation moderation = new Moderation();

    @Data
    public static class Storage {
        /** 存储类型：local | fastdfs */
        private String type = "local";
        /** 本地存储根目录 */
        private String localPath = "./upload";
        /** 静态资源访问域名（生产为 Nginx 域名） */
        private String accessDomain = "http://localhost:8080";
    }

    @Data
    public static class FastDfs {
        /** tracker 服务器地址列表，逗号分隔 */
        private String trackerServers = "192.168.1.100:22122";
        /** 连接超时（秒） */
        private int connectTimeout = 5;
        /** 网络超时（秒） */
        private int networkTimeout = 30;
    }

    @Data
    public static class RocketMq {
        /** 是否启用 RocketMQ（false 时消息直接本地处理落库，便于无 broker 环境开发） */
        private boolean enabled = false;
        /** NameServer 地址 */
        private String nameServer = "127.0.0.1:9876";
        /** 生产者组 */
        private String producerGroup = "bilbil-producer-group";
        /** 消费者组 */
        private String consumerGroup = "bilbil-consumer-group";
    }

    @Data
    public static class Token {
        /** token 有效期（天） */
        private int expireDays = 7;
    }

    @Data
    public static class Moderation {
        /** 是否启用内容审核（false 时直接放行） */
        private boolean enabled = true;
        /** 文本审核：DeepSeek API Key（环境变量 DEEPSEEK_API_KEY） */
        private String textApiKey = "";
        /** 文本审核：OpenAI 兼容接口地址 */
        private String textBaseUrl = "https://api.deepseek.com";
        /** 文本审核：模型名称 */
        private String textModel = "deepseek-chat";
        /** 视觉审核：通义千问 Qwen-VL API Key（环境变量 DASHSCOPE_API_KEY） */
        private String visionApiKey = "";
        /** 视觉审核：OpenAI 兼容接口地址（DashScope） */
        private String visionBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        /** 视觉审核：模型名称 */
        private String visionModel = "qwen-vl-max";
        /** 视频抽帧数量 */
        private int frameCount = 3;
        /** 单次审核超时（秒） */
        private int timeoutSeconds = 120;
    }
}
