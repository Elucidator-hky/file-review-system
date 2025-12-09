package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Redis 缓存监控响应。
 */
@Data
public class CacheMonitorResponse {

    private long totalKeys;
    private long hits;
    private long misses;
    private double hitRate;
    private String usedMemoryHuman;
    private long usedMemoryBytes;
    private long connectedClients;
    private LocalDateTime generatedAt;
    private List<CacheKeySnapshot> hotKeys = Collections.emptyList();

    @Data
    public static class CacheKeySnapshot {
        private String key;
        private Long ttlSeconds;
    }
}
