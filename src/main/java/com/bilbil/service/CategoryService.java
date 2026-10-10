package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.entity.VideoCategory;
import com.bilbil.mapper.VideoCategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 视频分区服务
 * <p>分区列表变化频率极低，适合缓存到 Redis，避免每次都查库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final VideoCategoryMapper videoCategoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<VideoCategory>> LIST_TYPE = new TypeReference<>() {
    };

    /**
     * 查询所有分区（优先读 Redis 缓存）
     */
    public List<VideoCategory> list() {
        try {
            String cached = redisTemplate.opsForValue().get(RedisKeyConstant.CATEGORY_LIST_KEY);
            if (StrUtil.isNotBlank(cached)) {
                return objectMapper.readValue(cached, LIST_TYPE);
            }
        } catch (Exception e) {
            log.warn("分区缓存读取失败，回源数据库", e);
        }
        List<VideoCategory> list = videoCategoryMapper.selectAll();
        try {
            redisTemplate.opsForValue().set(RedisKeyConstant.CATEGORY_LIST_KEY,
                    objectMapper.writeValueAsString(list), Duration.ofDays(1));
        } catch (Exception e) {
            log.warn("分区缓存写入失败", e);
        }
        return list;
    }

    /**
     * 根据ID查询分区
     */
    public VideoCategory getById(Long id) {
        if (id == null) {
            return null;
        }
        return list().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseGet(() -> videoCategoryMapper.selectById(id));
    }
}
