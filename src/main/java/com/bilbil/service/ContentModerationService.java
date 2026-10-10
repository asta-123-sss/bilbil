package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.bilbil.config.BilbilProperties;
import com.bilbil.dto.ModerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

/**
 * AI 内容审核服务（发布视频 / 广告前调用）。
 *
 * <p>文本审核走 DeepSeek（OpenAI 兼容），视觉审核（封面图 + 视频抽帧）走通义千问 Qwen-VL
 * （DashScope OpenAI 兼容接口）。两者都只发一个标准的 {@code /chat/completions} 请求，
 * 直接复用 Hutool + Jackson，不引入 LangChain4j 等额外依赖。</p>
 *
 * <p>降级策略：未配置对应 API Key 时对应环节自动跳过；调用/解析异常时 fail-open 放行并告警，
 * 避免 AI 抖动挡住正常发布。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private static final String TEXT_SYSTEM =
            "你是视频内容审核员。请审核用户提交的标题、简介、标签，判断是否含违规内容。\n"
            + "违规类别：涉政敏感、色情低俗、暴力血腥、赌博、诈骗、违法违规广告、人身攻击、侵权盗版、其他违法信息。\n"
            + "要求：只返回一个 JSON，不要输出其它任何文字，格式：{\"passed\": true, \"reason\": \"\"}\n"
            + "passed 为 true 表示通过；为 false 表示不通过，reason 用一句简短中文说明具体违规点和原因（通过时为空字符串）。";

    private static final String VISION_SYSTEM =
            "你是图片内容审核员。请审核图片是否含违规内容。\n"
            + "违规类别：色情低俗、血腥暴力、恐怖惊悚、涉政敏感、违法广告、赌博诈骗、其他违法信息。\n"
            + "要求：只返回一个 JSON，不要输出其它任何文字，格式：{\"passed\": true, \"reason\": \"\"}\n"
            + "passed 为 true 表示通过；为 false 表示不通过，reason 用一句简短中文说明具体违规点和原因（通过时为空字符串）。";

    private final BilbilProperties properties;
    private final ObjectMapper objectMapper;
    private final FrameExtractor frameExtractor;

    /** 审核待发布的视频：文字 → 封面图 → 视频抽帧，任一环节不通过即驳回 */
    public ModerationResult moderateVideo(String title, String description, String tags,
                                          String coverUrl, byte[] videoBytes) {
        if (!isTextEnabled()) {
            return ModerationResult.pass();
        }

        ModerationResult text = checkText(title, description, tags);
        if (!text.passed()) {
            return text;
        }

        if (isVisionEnabled() && StrUtil.isNotBlank(coverUrl)) {
            ModerationResult cover = checkImage(coverUrl, "视频封面图");
            if (!cover.passed()) {
                return cover;
            }
        }

        if (isVisionEnabled() && videoBytes != null) {
            List<byte[]> frames = frameExtractor.extract(videoBytes, properties.getModeration().getFrameCount());
            for (int i = 0; i < frames.size(); i++) {
                ModerationResult fr = checkImage(frames.get(i), "视频第" + (i + 1) + "帧");
                if (!fr.passed()) {
                    return fr;
                }
            }
        }
        return ModerationResult.pass();
    }

    /** 审核待发布的广告：文字 → 广告图 */
    public ModerationResult moderateAd(String title, String coverUrl) {
        if (!isTextEnabled()) {
            return ModerationResult.pass();
        }

        ModerationResult text = checkText(title, null, null);
        if (!text.passed()) {
            return text;
        }

        if (isVisionEnabled() && StrUtil.isNotBlank(coverUrl)) {
            return checkImage(coverUrl, "广告图");
        }
        return ModerationResult.pass();
    }

    // ---------- 文本审核（DeepSeek） ----------

    private ModerationResult checkText(String title, String description, String tags) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("标题：").append(StrUtil.nullToEmpty(title)).append('\n');
            sb.append("简介：").append(StrUtil.nullToEmpty(description)).append('\n');
            sb.append("标签：").append(StrUtil.nullToEmpty(tags));
            String content = callChat(properties.getModeration().getTextBaseUrl(),
                    properties.getModeration().getTextApiKey(),
                    properties.getModeration().getTextModel(),
                    TEXT_SYSTEM, sb.toString());
            return parse(content);
        } catch (Exception e) {
            log.warn("文本审核调用失败，放行: {}", e.getMessage());
            return ModerationResult.pass();
        }
    }

    // ---------- 视觉审核（Qwen-VL） ----------

    private ModerationResult checkImage(String imageUrl, String label) {
        try {
            String content = callVision(properties.getModeration().getVisionBaseUrl(),
                    properties.getModeration().getVisionApiKey(),
                    properties.getModeration().getVisionModel(),
                    imageUrl, label);
            return parse(content);
        } catch (Exception e) {
            log.warn("图片审核调用失败，放行: {}", e.getMessage());
            return ModerationResult.pass();
        }
    }

    private ModerationResult checkImage(byte[] jpegBytes, String label) {
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpegBytes);
        return checkImage(dataUrl, label);
    }

    // ---------- 底层 OpenAI 兼容调用 ----------

    /** 纯文本对话：POST {baseUrl}/chat/completions */
    private String callChat(String baseUrl, String apiKey, String model, String system, String user) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", system);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", user);
        body.put("temperature", 0.1);

        String json = postChat(StrUtil.removeSuffix(baseUrl, "/") + "/chat/completions", apiKey, body);
        JsonNode root = objectMapper.readTree(json);
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    /** 视觉对话：图片以 image_url 形式传入（支持 http(s) 地址或 data:image/...;base64,...） */
    private String callVision(String baseUrl, String apiKey, String model, String imageUrl, String label) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        ArrayNode content = usr.putArray("content");
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", VISION_SYSTEM + "\n图片类型：" + label);
        ObjectNode imgPart = content.addObject();
        imgPart.put("type", "image_url");
        ObjectNode imageUrlNode = imgPart.putObject("image_url");
        imageUrlNode.put("url", imageUrl);
        body.put("temperature", 0.1);

        String json = postChat(StrUtil.removeSuffix(baseUrl, "/") + "/chat/completions", apiKey, body);
        JsonNode root = objectMapper.readTree(json);
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    private String postChat(String url, String apiKey, ObjectNode body) {
        int timeoutMs = Math.max(1000, properties.getModeration().getTimeoutSeconds() * 1000);
        HttpResponse resp = null;
        try {
            resp = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(timeoutMs)
                    .execute();
            if (resp.getStatus() != 200) {
                throw new IllegalStateException("HTTP " + resp.getStatus() + ": " + StrUtil.sub(resp.body(), 0, 200));
            }
            return resp.body();
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }

    // ---------- 解析 ----------

    /** 解析模型返回的 {@code {"passed":bool,"reason":"..."}}，解析失败 fail-open */
    private ModerationResult parse(String content) {
        if (StrUtil.isBlank(content)) {
            return ModerationResult.pass();
        }
        try {
            return toResult(objectMapper.readTree(content));
        } catch (Exception e) {
            // 模型偶尔会包一层说明文字，尝试抽取其中的 JSON 片段
            int s = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (s >= 0 && end > s) {
                try {
                    return toResult(objectMapper.readTree(content.substring(s, end + 1)));
                } catch (Exception ignored) {
                }
            }
            log.warn("审核结果解析失败，放行: {}", StrUtil.sub(content, 0, 120));
            return ModerationResult.pass();
        }
    }

    private ModerationResult toResult(JsonNode node) {
        boolean passed = node.path("passed").asBoolean(true);
        String reason = node.path("reason").asText("");
        return new ModerationResult(passed, StrUtil.emptyToNull(reason));
    }

    // ---------- 开关 ----------

    private boolean isTextEnabled() {
        BilbilProperties.Moderation m = properties.getModeration();
        return m != null && m.isEnabled() && StrUtil.isNotBlank(m.getTextApiKey());
    }

    private boolean isVisionEnabled() {
        BilbilProperties.Moderation m = properties.getModeration();
        return m != null && m.isEnabled() && StrUtil.isNotBlank(m.getVisionApiKey());
    }
}
