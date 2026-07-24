package com.javaacademy.platform.ai;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window Redis rate limiter for AI hint requests.
 *
 * <p>Key: {@code rate:ai-hint:{userId}:{hourBucket}} where {@code hourBucket} is
 * {@code epochMillis / 3_600_000}. The key expires after 2 hours to prevent memory leaks.
 * This is a fixed-window approximation — a true sliding window would require a sorted set
 * per user, which is sufficient for the current load targets.
 */
@Component
@RequiredArgsConstructor
public class AiRateLimiter {

    public static final int MAX_HINTS_PER_HOUR = 20;
    static final long KEY_TTL_SECONDS = 7200L;
    static final String KEY_PREFIX = "rate:ai-hint:";

    private final StringRedisTemplate redis;

    /**
     * Returns {@code true} if the user is allowed to request a hint now, {@code false} if
     * the hourly limit is reached.
     *
     * <p>Increments the counter atomically. If the counter just reached 1 (first request in
     * the window), sets the TTL so the key self-expires.
     */
    public boolean isAllowed(UUID userId) {
        String key = buildKey(userId);
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redis.expire(key, java.time.Duration.ofSeconds(KEY_TTL_SECONDS));
        }
        return count <= MAX_HINTS_PER_HOUR;
    }

    static String buildKey(UUID userId) {
        long hourBucket = System.currentTimeMillis() / 3_600_000L;
        return KEY_PREFIX + userId + ":" + hourBucket;
    }
}
