package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.config.BilbilProperties;
import com.bilbil.entity.Ad;
import com.bilbil.entity.VideoCategory;
import com.bilbil.mapper.VideoMapper;
import com.bilbil.mapper.WatchHistoryMapper;
import com.bilbil.vo.CategoryStatVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 智能广告投放服务：根据用户兴趣画像，用 DeepSeek 对候选广告做相关度排序。
 *
 * <p>降级策略：未启用 / 未配 Key / 冷启动无行为信号 / 调用或解析失败时返回 null，
 * 由 {@link AdService} 回退到规则式投放，保证广告始终能正常下发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdRankingService {

    private static final String SYSTEM_PROMPT =
            "你是广告投放智能助手。请根据用户的兴趣画像，从候选广告中挑选出最相关、最可能吸引该用户的广告，"
            + "并按相关度从高到低排序。\n"
            + "要求：只返回一个 JSON，不要输出其它任何文字，格式：{\"adIds\": [3, 1, 5], \"reasons\": [\"理由1\", \"理由2\"]}\n"
            + "adIds 是候选广告的 id 列表（按相关度降序，最多 5 个）；reasons 是与 adIds 一一对应的简短中文推荐理由。";

    private final BilbilProperties properties;
    private final ObjectMapper objectMapper;
    private final VideoMapper videoMapper;
    private final CategoryService categoryService;
    private final WatchHistoryMapper watchHistoryMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 对候选广告做智能排序，返回按相关度降序的广告 id（最多 topN 条）。
     * 返回 null 表示应回退到规则式投放。
     */
    public List<Long> rank(Long userId, List<Ad> candidates) {
        BilbilProperties.AdRanking cfg = properties.getAdRanking();
        if (userId == null || cfg == null || !cfg.isEnabled() || StrUtil.isBlank(cfg.getApiKey())) {
            return null;
        }
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // 命中缓存直接复用排序结果（曝光计费由调用方逐次执行，这里只缓存顺序）
        List<Long> cached = readCache(userId);
        if (cached != null) {
            return limit(filterValid(cached, candidates), cfg.getTopN());
        }

        // 用户画像：偏好分区 + 近期观看标题
        List<CategoryStatVO> stats = videoMapper.selectUserCategoryStats(userId);
        List<String> recentTitles = watchHistoryMapper.selectRecentTitles(userId, 15);
        boolean hasSignal = (stats != null && !stats.isEmpty())
                || (recentTitles != null && !recentTitles.isEmpty());
        if (!hasSignal) {
            return null; // 冷启动无行为信号 → 规则式
        }

        try {
            Map<Long, String> catNameMap = categoryService.list().stream()
                    .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));
            String prompt = buildUserPrompt(stats, recentTitles, candidates, catNameMap);
            String content = chat(cfg, SYSTEM_PROMPT, prompt);
            List<Long> adIds = parseAdIds(content);
            if (adIds == null || adIds.isEmpty()) {
                return null;
            }
            List<Long> ordered = limit(filterValid(adIds, candidates), cfg.getTopN());
            if (ordered.isEmpty()) {
                return null;
            }
            writeCache(userId, ordered);
            log.info("智能广告排序成功, userId={}, 排序结果={}", userId, ordered);
            return ordered;
        } catch (Exception e) {
            log.warn("智能广告排序失败，回退规则式投放: {}", e.getMessage());
            return null;
        }
    }

    // ---------- 画像与提示词 ----------

    private String buildUserPrompt(List<CategoryStatVO> stats, List<String> recentTitles,
                                   List<Ad> candidates, Map<Long, String> catNameMap) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode user = root.putObject("user");

        ArrayNode prefs = user.putArray("preferredCategories");
        if (stats != null) {
            for (CategoryStatVO s : stats) {
                ObjectNode c = prefs.addObject();
                c.put("name", catNameMap.getOrDefault(s.getCategoryId(), "未知"));
                c.put("score", s.getScore() == null ? 0 : s.getScore());
            }
        }

        ArrayNode titles = user.putArray("recentWatchedTitles");
        if (recentTitles != null) {
            recentTitles.stream().filter(StrUtil::isNotBlank).forEach(titles::add);
        }

        ArrayNode ads = root.putArray("candidateAds");
        for (Ad a : candidates) {
            ObjectNode o = ads.addObject();
            o.put("id", a.getId());
            o.put("title", StrUtil.nullToEmpty(a.getTitle()));
            o.put("category", catNameMap.getOrDefault(a.getCategoryId(), "通用"));
        }
        return objectMapper.writeValueAsString(root);
    }

    // ---------- DeepSeek 调用（沿用内容审核服务的 Hutool + Jackson 写法） ----------

    private String chat(BilbilProperties.AdRanking cfg, String system, String user) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", cfg.getModel());
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", system);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", user);
        body.put("temperature", 0.1);

        int timeoutMs = Math.max(1000, cfg.getTimeoutSeconds() * 1000);
        HttpResponse resp = null;
        try {
            resp = HttpRequest.post(StrUtil.removeSuffix(cfg.getBaseUrl(), "/") + "/chat/completions")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(timeoutMs)
                    .execute();
            if (resp.getStatus() != 200) {
                throw new IllegalStateException("HTTP " + resp.getStatus() + ": " + StrUtil.sub(resp.body(), 0, 200));
            }
            JsonNode root = objectMapper.readTree(resp.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }

    // ---------- 结果解析与缓存 ----------

    private List<Long> parseAdIds(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        try {
            return extractAdIds(objectMapper.readTree(content));
        } catch (Exception e) {
            // 模型偶尔会包一层说明文字，尝试抽取其中的 JSON 片段
            int s = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (s >= 0 && end > s) {
                try {
                    return extractAdIds(objectMapper.readTree(content.substring(s, end + 1)));
                } catch (Exception ignored) {
                }
            }
            log.warn("智能广告排序结果解析失败: {}", StrUtil.sub(content, 0, 120));
            return null;
        }
    }

    private List<Long> extractAdIds(JsonNode node) {
        JsonNode ids = node.path("adIds");
        if (!ids.isArray()) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (JsonNode n : ids) {
            if (n.isNumber()) {
                result.add(n.asLong());
            }
        }
        return result;
    }

    private List<Long> filterValid(List<Long> ids, List<Ad> candidates) {
        Set<Long> valid = candidates.stream().map(Ad::getId).collect(Collectors.toSet());
        return ids.stream().filter(valid::contains).distinct().collect(Collectors.toList());
    }

    private List<Long> limit(List<Long> ids, int topN) {
        return ids.size() <= topN ? ids : ids.subList(0, topN);
    }

    private List<Long> readCache(Long userId) {
        try {
            String cached = redisTemplate.opsForValue().get(RedisKeyConstant.adRankKey(userId));
            if (StrUtil.isBlank(cached)) {
                return null;
            }
            return objectMapper.readValue(cached,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCache(Long userId, List<Long> ordered) {
        try {
            redisTemplate.opsForValue().set(RedisKeyConstant.adRankKey(userId),
                    objectMapper.writeValueAsString(ordered), Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("智能广告排序缓存写入失败", e);
        }
    }
}
