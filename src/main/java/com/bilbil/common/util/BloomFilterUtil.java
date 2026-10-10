package com.bilbil.common.util;

import cn.hutool.core.util.HashUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis Bitmap 的布隆过滤器
 * <p>用途：防止缓存穿透。视频ID在写入缓存/查询前先判断是否可能存在于布隆过滤器中，
 * 若不存在则直接返回，避免大量不存在ID击穿到数据库。</p>
 * <p>实现：使用双哈希（double hashing）生成 k 个位下标，通过 Redis setBit/getBit 操作位图。</p>
 */
@Component
public class BloomFilterUtil {

    /** 期望插入元素数量 */
    private static final long EXPECTED_INSERTIONS = 1_000_000L;
    /** 期望误判率 */
    private static final double FPP = 0.01;
    /** 位图大小（bit） */
    private static final int BIT_SIZE = optimalBitSize(EXPECTED_INSERTIONS, FPP);
    /** 哈希函数个数 */
    private static final int HASH_COUNT = optimalHashCount(EXPECTED_INSERTIONS, BIT_SIZE);

    private final StringRedisTemplate redisTemplate;

    public BloomFilterUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 向布隆过滤器添加元素
     *
     * @param key   bitmap 对应的 Redis key
     * @param value 元素值
     */
    public void add(String key, Object value) {
        int[] offsets = hashOffsets(String.valueOf(value));
        for (int offset : offsets) {
            redisTemplate.opsForValue().setBit(key, offset, true);
        }
    }

    /**
     * 判断元素是否可能存在（存在误判，不存在一定准确）
     *
     * @param key   bitmap 对应的 Redis key
     * @param value 元素值
     * @return true 可能存在；false 一定不存在
     */
    public boolean mightContain(String key, Object value) {
        int[] offsets = hashOffsets(String.valueOf(value));
        for (int offset : offsets) {
            Boolean bit = redisTemplate.opsForValue().getBit(key, offset);
            if (bit == null || !bit) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算元素对应的 k 个位下标（双哈希法）
     */
    private int[] hashOffsets(String value) {
        long hash1 = HashUtil.mixHash(value);
        long hash2 = HashUtil.fnvHash(value);
        int[] offsets = new int[HASH_COUNT];
        for (int i = 0; i < HASH_COUNT; i++) {
            long combined = hash1 + i * hash2;
            offsets[i] = (int) ((combined & Long.MAX_VALUE) % BIT_SIZE);
        }
        return offsets;
    }

    /** 由期望插入数量 n 和误判率 p 计算最优位图大小 m */
    private static int optimalBitSize(long n, double p) {
        return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /** 由期望插入数量 n 和位图大小 m 计算最优哈希函数个数 k */
    private static int optimalHashCount(long n, int m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }
}
