package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end adversarial tests that submit hostile code to the real Docker sandbox and assert
 * every attack is contained with the correct verdict. Each test is aborted gracefully when Docker
 * is unavailable or the runner image has not been built yet.
 */
class SandboxAdversarialTest {

    static final SandboxProperties PROPS = new SandboxProperties("java-ai-academy/runner:21", 134_217_728L, 5, 4);
    static final String IMAGE_NAME = "java-ai-academy/runner:21";

    static boolean dockerAndImageAvailable = false;
    static DockerCodeExecutionService sharedService;

    @BeforeAll
    static void detectEnvironment() {
        try {
            var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            var httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            var client = DockerClientImpl.getInstance(config, httpClient);
            client.pingCmd().exec();

            var images = client.listImagesCmd().withImageNameFilter(IMAGE_NAME).exec();
            if (images.isEmpty()) {
                return; // image not built — tests will be aborted
            }

            sharedService = new DockerCodeExecutionService(client, PROPS);
            dockerAndImageAvailable = true;
        } catch (Exception dockerUnavailable) {
            // Docker not running — tests will be aborted
        }
    }

    DockerCodeExecutionService service;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(dockerAndImageAvailable, "Docker not available or runner image not built — skipping");
        service = sharedService;
    }

    // ── attack: infinite loop ─────────────────────────────────────────────────

    @Test
    void infiniteLoop_exceedsTimeout_returnsTimeout() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() { while (true) {} }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        assertThat(result.status())
                .as("Infinite loop must be killed by the timeout, not report PASSED")
                .isEqualTo(ExecutionStatus.TIMEOUT);
    }

    // ── attack: fork bomb (PID-limit containment) ─────────────────────────────

    @Test
    void forkBomb_pidsLimitContains_returnsFailed() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() {
                        for (int i = 0; i < 1_000; i++) {
                            new Thread(() -> {
                                try { Thread.sleep(60_000); } catch (Exception ex) {}
                            }).start();
                        }
                        return 42;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        assertThat(result.status())
                .as("Thread fork bomb must be contained (pids-limit) and report FAILED, not PASSED")
                .isNotEqualTo(ExecutionStatus.PASSED);
    }

    // ── attack: allocation bomb (memory-limit containment) ────────────────────

    @Test
    void allocBomb_2GbAlloc_returnsFailed() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() {
                        byte[] bomb = new byte[2_000_000_000];
                        return bomb.length;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        // FAILED (OOM kills JVM) or TIMEOUT (slow memory zeroing in Docker Desktop VM) — both prove containment
        assertThat(result.status())
                .as("2 GB allocation must be contained (OOM or timeout) — must never report PASSED")
                .isNotEqualTo(ExecutionStatus.PASSED);
    }

    // ── attack: network call (--network=none containment) ─────────────────────

    @Test
    void networkCall_networkNone_returnsFailed() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() throws Exception {
                        new java.net.Socket("8.8.8.8", 53).close();
                        return 42;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        assertThat(result.status())
                .as("Network call must fail (--network=none) and report FAILED, not PASSED")
                .isEqualTo(ExecutionStatus.FAILED);
    }

    // ── attack: file write outside workdir (read-only rootfs) ─────────────────

    @Test
    void fileWriteOutsideWorkdir_readOnlyRootfs_returnsFailed() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() throws Exception {
                        java.nio.file.Files.writeString(java.nio.file.Path.of("/etc/evil.txt"), "pwned");
                        return 42;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        assertThat(result.status())
                .as("Write to /etc must fail (read-only rootfs) and report FAILED, not PASSED")
                .isEqualTo(ExecutionStatus.FAILED);
    }

    // ── attack: System.exit(0) (kills JVM before tests complete) ─────────────

    @Test
    void systemExitZero_noXmlGenerated_returnsFailed() {
        String solutionCode =
                """
                public class Solution {
                    public int compute() {
                        System.exit(0);
                        return 42;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        // On some JVM/Docker combinations, System.exit(0) causes shutdown hooks to hang, yielding
        // TIMEOUT. The invariant is that the verdict must never be PASSED.
        assertThat(result.status())
                .as("System.exit(0) must not produce a PASSED verdict — attack is not contained otherwise")
                .isNotEqualTo(ExecutionStatus.PASSED);
    }

    // ── attack: large stdout (log-cap containment) ───────────────────────────

    @Test
    void largeStdout_exceedsCap_sandboxDoesNotCrashAndLogsAreCapped() {
        // 200 KB in a single println — well above the 64 KB log cap.
        // On macOS Docker Desktop the pipe-flush overhead can cause a TIMEOUT, which is still
        // "contained" (not PASSED). The assertions verify: no sandbox crash (ERROR), and whatever
        // logs are captured respect the cap.
        String solutionCode =
                """
                public class Solution {
                    public int compute() {
                        System.out.println("A".repeat(200_000));
                        return 42;
                    }
                }
                """;

        ExecutionResult result = service.execute(request(solutionCode));

        assertThat(result.status())
                .as("Large stdout must not crash the sandbox — PASSED/FAILED/TIMEOUT are all acceptable")
                .isNotEqualTo(ExecutionStatus.ERROR);
        assertThat(result.logs().length())
                .as("Captured logs must not exceed the %d-byte cap", DockerCodeExecutionService.LOG_CAP_BYTES)
                .isLessThanOrEqualTo((int) DockerCodeExecutionService.LOG_CAP_BYTES);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static final String TEST_CODE =
            """
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            public class TaskTest {
                @Test
                void compute() { assertEquals(42, new Solution().compute()); }
            }
            """;

    private static ExecutionRequest request(String solutionCode) {
        return new ExecutionRequest(UUID.randomUUID(), solutionCode, TEST_CODE, "TaskTest");
    }
}
