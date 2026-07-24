package com.javaacademy.platform.sandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.util.JUnitXmlParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class DockerCodeExecutionService implements CodeExecutionEngine {

    static final String JUNIT_JAR = "/opt/junit-platform-console-standalone.jar";
    static final String CONTAINER_WORK_DIR = "/work";
    static final long LOG_CAP_BYTES = 65_536L;

    private final DockerClient dockerClient;
    private final SandboxProperties properties;
    private final Semaphore concurrencyLimit;

    public DockerCodeExecutionService(DockerClient dockerClient, SandboxProperties properties) {
        this.dockerClient = dockerClient;
        this.properties = properties;
        this.concurrencyLimit = new Semaphore(properties.maxConcurrent());
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        try {
            concurrencyLimit.acquire();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return ExecutionResult.error("Sandbox interrupted while waiting for capacity");
        }
        try {
            return runInContainer(request);
        } finally {
            concurrencyLimit.release();
        }
    }

    private ExecutionResult runInContainer(ExecutionRequest request) {
        Path hostWorkDir = null;
        String containerId = null;
        long startMs = System.currentTimeMillis();

        try {
            hostWorkDir = prepareWorkDir(request);
            containerId = createContainer(hostWorkDir, request.testClassName());
            dockerClient.startContainerCmd(containerId).exec();

            boolean timedOut = awaitExit(containerId);
            if (timedOut) {
                killQuietly(containerId);
                return ExecutionResult.timeout("Execution exceeded " + properties.timeoutSeconds() + "s limit");
            }

            String logs = collectLogs(containerId);
            long durationMs = System.currentTimeMillis() - startMs;

            var xmlResult = JUnitXmlParser.parseFailedTestCount(hostWorkDir, request.testClassName());
            if (xmlResult.isPresent()) {
                int failedTests = xmlResult.getAsInt();
                return failedTests == 0
                        ? ExecutionResult.passed(logs, durationMs)
                        : ExecutionResult.failed(failedTests, logs, durationMs);
            }
            // XML absent: compilation failed or JVM terminated prematurely (e.g. System.exit).
            // In either case tests did not complete — always FAILED.
            return ExecutionResult.failed(0, logs, durationMs);

        } catch (Exception exception) {
            log.error("Sandbox execution error for task {}", request.taskId(), exception);
            return ExecutionResult.error("Internal sandbox error: " + exception.getMessage());
        } finally {
            removeQuietly(containerId);
            deleteWorkDir(hostWorkDir);
        }
    }

    private Path prepareWorkDir(ExecutionRequest request) throws IOException {
        Path workDir = Files.createTempDirectory("sandbox-");
        // World-writable so container uid=1000 can write compiled class files
        Files.setPosixFilePermissions(workDir, PosixFilePermissions.fromString("rwxrwxrwx"));
        Files.writeString(workDir.resolve("Solution.java"), request.solutionCode());
        Files.writeString(workDir.resolve(request.testClassName() + ".java"), request.testCode());
        return workDir;
    }

    String createContainer(Path hostWorkDir, String testClassName) {
        return dockerClient
                .createContainerCmd(properties.image())
                .withHostConfig(buildHostConfig(hostWorkDir))
                .withUser("1000:0")
                .withWorkingDir(CONTAINER_WORK_DIR)
                .withCmd("sh", "-c", buildCommand(testClassName))
                .exec()
                .getId();
    }

    HostConfig buildHostConfig(Path hostWorkDir) {
        return HostConfig.newHostConfig()
                .withNetworkMode("none")
                .withMemory(properties.memoryBytes())
                .withMemorySwap(properties.memoryBytes())
                .withCpuPeriod(100_000L)
                .withCpuQuota(50_000L)
                .withPidsLimit(64L)
                .withReadonlyRootfs(true)
                .withCapDrop(Capability.ALL)
                .withSecurityOpts(List.of("no-new-privileges:true"))
                .withTmpFs(Map.of("/tmp", "size=64m,noexec"))
                .withBinds(new Bind(hostWorkDir.toString(), new Volume(CONTAINER_WORK_DIR), AccessMode.rw));
    }

    static String buildCommand(String testClassName) {
        return "javac -cp " + JUNIT_JAR + " /work/*.java -d /work 2>&1"
                + " && java -jar " + JUNIT_JAR
                + " execute"
                + " --classpath /work"
                + " --select-class " + testClassName
                + " --reports-dir /work"
                + " --disable-ansi-colors";
    }

    private boolean awaitExit(String containerId) {
        try {
            WaitContainerResultCallback callback =
                    dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
            boolean completed = callback.awaitCompletion(properties.timeoutSeconds(), TimeUnit.SECONDS);
            return !completed;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private String collectLogs(String containerId) {
        FrameCollector collector = new FrameCollector();
        try {
            dockerClient
                    .logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(collector)
                    .awaitCompletion();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        return collector.output();
    }

    private static final class FrameCollector extends ResultCallbackTemplate<FrameCollector, Frame> {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void onNext(Frame frame) {
            byte[] payload = frame.getPayload();
            if (payload != null && (buffer.size() + payload.length) <= LOG_CAP_BYTES) {
                buffer.writeBytes(payload);
            }
        }

        String output() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private void killQuietly(String containerId) {
        if (containerId == null) {
            return;
        }
        try {
            dockerClient.killContainerCmd(containerId).exec();
        } catch (Exception killException) {
            log.warn("Failed to kill container {}", containerId);
        }
    }

    private void removeQuietly(String containerId) {
        if (containerId == null) {
            return;
        }
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception removeException) {
            log.warn("Failed to remove container {}", containerId);
        }
    }

    private static void deleteWorkDir(Path workDir) {
        if (workDir == null) {
            return;
        }
        try {
            Files.walk(workDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ioException) {
            log.warn("Failed to delete sandbox work dir {}", workDir);
        }
    }
}
