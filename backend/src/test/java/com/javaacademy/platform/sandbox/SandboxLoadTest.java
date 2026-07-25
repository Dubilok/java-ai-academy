package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Load test: 100 concurrent submission threads, assert p95 container execution time < 5 s,
 * and assert 0 containers leaked after all complete.
 *
 * <p>Skipped gracefully when Docker is unavailable or the runner image has not been built.
 * Each thread blocks on a {@link CountDownLatch} start gate so all 100 requests enter the
 * service concurrently. The service semaphore limits actual container concurrency to
 * {@code maxConcurrent} (8 by default); the measured {@code durationMs} is the container
 * execution time only, independent of queue wait time.
 */
class SandboxLoadTest {

    static final String IMAGE_NAME = "java-ai-academy/runner:21";
    static final int SUBMISSION_COUNT = 100;
    static final int THREAD_POOL_SIZE = 100;
    static final long TOTAL_TIMEOUT_SECONDS = 300; // 5 minutes budget for all 100 to finish
    static final long P95_LIMIT_MS = 5_000; // sandbox timeout is 5 s; p95 must stay under it

    static final SandboxProperties PROPS = new SandboxProperties(IMAGE_NAME, 134_217_728L, 5, 8);

    static boolean dockerAndImageAvailable = false;
    static DockerCodeExecutionService sharedService;
    static DockerClient sharedClient;

    @BeforeAll
    static void detectEnvironment() {
        try {
            var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            var httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            DockerClient client = DockerClientImpl.getInstance(config, httpClient);
            client.pingCmd().exec();

            List<?> images =
                    client.listImagesCmd().withImageNameFilter(IMAGE_NAME).exec();
            if (images.isEmpty()) {
                return; // image not built — tests will be aborted
            }

            sharedClient = client;
            sharedService = new DockerCodeExecutionService(client, PROPS);
            dockerAndImageAvailable = true;
        } catch (Exception dockerUnavailable) {
            // Docker not running — tests will be aborted
        }
    }

    @Test
    void concurrentSubmissions_100concurrent_p95Under5s_noContainerLeak() throws InterruptedException {
        Assumptions.assumeTrue(dockerAndImageAvailable, "Docker not available or runner image not built — skipping");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(SUBMISSION_COUNT);

        ExecutionResult[] results = new ExecutionResult[SUBMISSION_COUNT];
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int submissionIdx = 0; submissionIdx < SUBMISSION_COUNT; submissionIdx++) {
            int resultIndex = submissionIdx;
            executor.submit(() -> {
                try {
                    startGate.await(); // hold until all threads are ready
                    results[resultIndex] = sharedService.execute(passingRequest());
                } catch (Throwable throwable) {
                    unexpectedErrors.add(throwable);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // release all 100 threads simultaneously
        boolean allCompleted = doneLatch.await(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // ── correctness assertions ────────────────────────────────────────────

        assertThat(allCompleted)
                .as("All %d submissions must complete within %d seconds", SUBMISSION_COUNT, TOTAL_TIMEOUT_SECONDS)
                .isTrue();

        assertThat(unexpectedErrors)
                .as("No submission should throw an uncaught exception under concurrent load")
                .isEmpty();

        long nonPassedCount = Arrays.stream(results)
                .filter(result -> result != null && result.status() != ExecutionStatus.PASSED)
                .count();
        assertThat(nonPassedCount)
                .as("Correct code must always PASS under load — non-PASSED count")
                .isZero();

        // ── p95 execution time assertion ──────────────────────────────────────
        // durationMs is the container execution time (not queue wait); it is 0 for TIMEOUT/ERROR,
        // but those are already asserted away above.

        long[] sortedDurations = Arrays.stream(results)
                .mapToLong(ExecutionResult::durationMs)
                .sorted()
                .toArray();

        int p95Index = (int) Math.ceil(0.95 * sortedDurations.length) - 1;
        long p95DurationMs = sortedDurations[p95Index];

        assertThat(p95DurationMs)
                .as(
                        "p95 container execution time must stay under the 5 s timeout even under load "
                                + "(actual p95: %d ms, index: %d)",
                        p95DurationMs, p95Index)
                .isLessThan(P95_LIMIT_MS);

        // ── no-leak assertion ─────────────────────────────────────────────────

        List<?> remainingContainers = sharedClient
                .listContainersCmd()
                .withShowAll(true)
                .withAncestorFilter(List.of(IMAGE_NAME))
                .exec();

        assertThat(remainingContainers)
                .as(
                        "Zero containers should remain after all %d submissions complete — leaked container count: %d",
                        SUBMISSION_COUNT, remainingContainers.size())
                .isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static final String PASSING_SOLUTION =
            """
            public class Solution {
                public static int value() { return 42; }
            }
            """;

    private static final String PASSING_TEST =
            """
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            public class TaskTest {
                @Test
                void value_returns42() {
                    assertEquals(42, Solution.value());
                }
            }
            """;

    private static ExecutionRequest passingRequest() {
        return new ExecutionRequest(UUID.randomUUID(), PASSING_SOLUTION, PASSING_TEST, "TaskTest");
    }
}
