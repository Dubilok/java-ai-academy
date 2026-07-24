package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisSubmissionQueueTest {

    StringRedisTemplate redis;
    ListOperations<String, String> listOps;
    RedisSubmissionQueue queue;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(listOps);
        queue = new RedisSubmissionQueue(redis);
    }

    @Test
    void enqueue_pushesStringifiedUuidToRightOfList() {
        UUID submissionId = UUID.randomUUID();
        queue.enqueue(submissionId);
        verify(listOps).rightPush(eq(RedisSubmissionQueue.QUEUE_KEY), eq(submissionId.toString()));
    }

    @Test
    void dequeue_returnsUuidWhenRedisHasValue() {
        UUID submissionId = UUID.randomUUID();
        when(listOps.leftPop(anyString(), any(Duration.class))).thenReturn(submissionId.toString());
        Optional<UUID> result = queue.dequeue(Duration.ofSeconds(1));
        assertThat(result).hasValue(submissionId);
    }

    @Test
    void dequeue_returnsEmptyWhenRedisReturnsNull() {
        when(listOps.leftPop(anyString(), any(Duration.class))).thenReturn(null);
        Optional<UUID> result = queue.dequeue(Duration.ofSeconds(1));
        assertThat(result).isEmpty();
    }

    @Test
    void dequeue_usesConfiguredQueueKey() {
        when(listOps.leftPop(anyString(), any(Duration.class))).thenReturn(null);
        queue.dequeue(Duration.ofSeconds(2));
        verify(listOps).leftPop(eq(RedisSubmissionQueue.QUEUE_KEY), any(Duration.class));
    }
}
