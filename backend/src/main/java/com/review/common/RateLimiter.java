package com.review.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的简单限流工具。
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 在指定时间窗口内最多允许 limit 次请求。
     *
     * @param key    业务 key
     * @param limit  限制次数
     * @param window 时间窗口
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        String redisKey = "rl:" + key;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(redisKey, window);
        }
        return count != null && count <= limit;
    }
}
