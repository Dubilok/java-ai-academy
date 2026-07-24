package com.javaacademy.platform.sandbox;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSubmissionQueue implements SubmissionQueue {

    static final String QUEUE_KEY = "sandbox:submission-queue";

    private final StringRedisTemplate redis;

    @Override
    public void enqueue(UUID submissionId) {
        redis.opsForList().rightPush(QUEUE_KEY, submissionId.toString());
    }

    @Override
    public Optional<UUID> dequeue(Duration timeout) {
        String value = redis.opsForList().leftPop(QUEUE_KEY, timeout);
        return Optional.ofNullable(value).map(UUID::fromString);
    }
}
