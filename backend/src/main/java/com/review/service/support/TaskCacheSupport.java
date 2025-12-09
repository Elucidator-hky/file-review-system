package com.review.service.support;

import com.review.common.CacheConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 审查任务相关的缓存辅助类，负责常用 Key 的生成、读取与失效。
 */
@Component
@RequiredArgsConstructor
public class TaskCacheSupport {

    private final RedisTemplate<String, Object> redisTemplate;

    public Object read(String key) {
        if (redisTemplate == null || key == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    public void cache(String key, Object value, Duration ttl) {
        if (redisTemplate == null || key == null || ttl == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, randomTtl(ttl));
    }

    public void evict(String key) {
        if (redisTemplate == null || key == null) {
            return;
        }
        redisTemplate.delete(key);
    }

    /**
     * 删除相关人员的统计缓存，确保任务状态变更后读取的是实时数据。
     */
    public void evictStatistics(Long tenantId, Long creatorId, Long reviewerId) {
        if (redisTemplate == null || tenantId == null) {
            return;
        }
        if (creatorId != null) {
            redisTemplate.delete(userStatKey(tenantId, creatorId));
        }
        if (reviewerId != null) {
            redisTemplate.delete(reviewerStatKey(tenantId, reviewerId));
        }
    }

    public String userStatKey(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return null;
        }
        return CacheConstants.STAT_USER_PREFIX + tenantId + ":" + userId;
    }

    public String reviewerStatKey(Long tenantId, Long reviewerId) {
        if (tenantId == null || reviewerId == null) {
            return null;
        }
        return CacheConstants.STAT_REVIEWER_PREFIX + tenantId + ":" + reviewerId;
    }

    public String reviewerOptionKey(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return CacheConstants.REVIEWER_OPTION_PREFIX + tenantId;
    }

    public String tenantListKey(String suffix) {
        return CacheConstants.TENANT_LIST_PREFIX + (suffix == null ? "" : suffix);
    }

    public String taskMissKey(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return CacheConstants.TASK_MISS_PREFIX + taskId;
    }

    public String versionMissKey(Long versionId) {
        if (versionId == null) {
            return null;
        }
        return CacheConstants.VERSION_MISS_PREFIX + versionId;
    }

    public void evictByPrefix(String prefix) {
        if (redisTemplate == null || prefix == null) {
            return;
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Duration randomTtl(Duration ttl) {
        long base = ttl.getSeconds();
        long delta = Math.max(1, base / 5);
        long extra = ThreadLocalRandom.current().nextLong(delta);
        return Duration.ofSeconds(base + extra);
    }
}
