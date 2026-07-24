package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.progress.service.SubmissionWorker;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisSubmissionQueueIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    // Prevent the background worker from consuming queue items during tests
    @MockBean
    SubmissionWorker submissionWorker;

    @Autowired
    RedisSubmissionQueue queue;

    @Test
    void enqueueAndDequeue_roundTrip_returnsOriginalId() {
        UUID submissionId = UUID.randomUUID();
        queue.enqueue(submissionId);
        Optional<UUID> dequeued = queue.dequeue(Duration.ofSeconds(2));
        assertThat(dequeued).hasValue(submissionId);
    }

    @Test
    void dequeue_emptyQueue_returnsEmptyWithinTimeout() {
        Optional<UUID> dequeued = queue.dequeue(Duration.ofMillis(200));
        assertThat(dequeued).isEmpty();
    }

    @Test
    void enqueue_multipleItems_preservesFifoOrder() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        queue.enqueue(firstId);
        queue.enqueue(secondId);

        Optional<UUID> first = queue.dequeue(Duration.ofSeconds(1));
        Optional<UUID> second = queue.dequeue(Duration.ofSeconds(1));

        assertThat(first).hasValue(firstId);
        assertThat(second).hasValue(secondId);
    }
}
