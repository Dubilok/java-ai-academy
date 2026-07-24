package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DockerCodeExecutionServiceTest {

    static final UUID TASK_ID = UUID.randomUUID();
    static final SandboxProperties PROPS = new SandboxProperties("java-ai-academy/runner:21", 134_217_728L, 5, 4);

    DockerClient dockerClient;
    DockerCodeExecutionService service;

    // Command mocks — RETURNS_SELF handles the fluent withXxx() builder calls
    CreateContainerCmd createCmd;
    CreateContainerResponse createResponse;
    StartContainerCmd startCmd;
    WaitContainerCmd waitCmd;
    WaitContainerResultCallback waitCallback;
    LogContainerCmd logCmd;
    InspectContainerCmd inspectCmd;
    InspectContainerResponse inspectResponse;
    InspectContainerResponse.ContainerState containerState;
    RemoveContainerCmd removeCmd;
    KillContainerCmd killCmd;

    @BeforeEach
    void setUp() throws InterruptedException {
        dockerClient = mock(DockerClient.class);
        service = new DockerCodeExecutionService(dockerClient, PROPS);

        createCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("c123");
        doReturn(createResponse).when(createCmd).exec();
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);

        startCmd = mock(StartContainerCmd.class, RETURNS_SELF);
        when(dockerClient.startContainerCmd(anyString())).thenReturn(startCmd);

        waitCmd = mock(WaitContainerCmd.class, RETURNS_SELF);
        waitCallback = mock(WaitContainerResultCallback.class);
        when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(true);
        doReturn(waitCallback).when(waitCmd).exec(any(WaitContainerResultCallback.class));
        when(dockerClient.waitContainerCmd(anyString())).thenReturn(waitCmd);

        logCmd = mock(LogContainerCmd.class, RETURNS_SELF);
        // exec() signals onComplete() immediately so awaitCompletion() returns without hanging
        doAnswer(inv -> {
                    var cb = (com.github.dockerjava.api.async.ResultCallbackTemplate<?, ?>) inv.getArgument(0);
                    cb.onComplete();
                    return cb;
                })
                .when(logCmd)
                .exec(any());
        when(dockerClient.logContainerCmd(anyString())).thenReturn(logCmd);

        containerState = mock(InspectContainerResponse.ContainerState.class);
        inspectResponse = mock(InspectContainerResponse.class);
        when(inspectResponse.getState()).thenReturn(containerState);
        inspectCmd = mock(InspectContainerCmd.class, RETURNS_SELF);
        doReturn(inspectResponse).when(inspectCmd).exec();
        when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectCmd);

        removeCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);
        when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeCmd);

        killCmd = mock(KillContainerCmd.class, RETURNS_SELF);
        when(dockerClient.killContainerCmd(anyString())).thenReturn(killCmd);
    }

    // ── buildCommand ─────────────────────────────────────────────────────────

    @Test
    void buildCommand_containsJavacAndJunitExecute() {
        String command = DockerCodeExecutionService.buildCommand("TaskTest");
        assertThat(command).contains("javac").contains("junit-platform-console-standalone.jar");
        assertThat(command).contains("execute").contains("--select-class TaskTest");
    }

    @Test
    void buildCommand_compilesBeforeRunningViaShellAnd() {
        String command = DockerCodeExecutionService.buildCommand("TaskTest");
        int javacIdx = command.indexOf("javac");
        int executeIdx = command.indexOf(" && ");
        int junitIdx = command.indexOf("execute");
        assertThat(javacIdx).isLessThan(executeIdx);
        assertThat(executeIdx).isLessThan(junitIdx);
    }

    // ── ExecutionResult factory methods ──────────────────────────────────────

    @Test
    void executionResult_passed_hasCorrectStatus() {
        ExecutionResult result = ExecutionResult.passed("ok", 100L);
        assertThat(result.status()).isEqualTo(ExecutionStatus.PASSED);
        assertThat(result.logs()).isEqualTo("ok");
        assertThat(result.durationMs()).isEqualTo(100L);
    }

    @Test
    void executionResult_failed_hasCorrectStatus() {
        ExecutionResult result = ExecutionResult.failed(2, "err", 200L);
        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void executionResult_timeout_hasZeroDuration() {
        ExecutionResult result = ExecutionResult.timeout("too slow");
        assertThat(result.status()).isEqualTo(ExecutionStatus.TIMEOUT);
        assertThat(result.durationMs()).isZero();
    }

    @Test
    void executionResult_error_hasZeroDuration() {
        ExecutionResult result = ExecutionResult.error("boom");
        assertThat(result.status()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(result.durationMs()).isZero();
    }

    // ── execute — verdict from XML (no XML = always FAILED) ──────────────────

    @Test
    void execute_noXmlReport_returnsFailed() {
        // No JUnit XML is written by the mocked Docker run, so verdict is always FAILED
        // (covers compilation failure AND System.exit() which also produce no XML)
        ExecutionResult result = service.execute(request("class Solution{}", "class TaskTest{}"));
        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
    }

    // ── execute — container lifecycle ────────────────────────────────────────

    @Test
    void execute_alwaysRemovesContainer_evenOnError() {
        when(dockerClient.startContainerCmd(anyString())).thenThrow(new RuntimeException("boom"));
        service.execute(request("class Solution{}", "class TaskTest{}"));
        verify(removeCmd).exec();
    }

    @Test
    void execute_dockerCreateFails_returnsError() {
        when(dockerClient.createContainerCmd(anyString())).thenThrow(new RuntimeException("no docker"));
        ExecutionResult result = service.execute(request("class Solution{}", "class TaskTest{}"));
        assertThat(result.status()).isEqualTo(ExecutionStatus.ERROR);
        verify(dockerClient, never()).startContainerCmd(anyString());
    }

    // ── execute — timeout ─────────────────────────────────────────────────────

    @Test
    void execute_containerTimesOut_returnsTimeoutAndKills() throws InterruptedException {
        when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(false); // not completed
        ExecutionResult result = service.execute(request("class Solution{}", "class TaskTest{}"));
        assertThat(result.status()).isEqualTo(ExecutionStatus.TIMEOUT);
        verify(killCmd).exec();
    }

    // ── execute — concurrency semaphore ──────────────────────────────────────

    @Test
    void execute_concurrencyLimitRespected_semaphoreAcquiredAndReleased() {
        when(containerState.getExitCodeLong()).thenReturn(0L);
        // Should complete without deadlock (semaphore released in finally)
        for (int iteration = 0; iteration < PROPS.maxConcurrent() + 1; iteration++) {
            service.execute(request("class Solution{}", "class TaskTest{}"));
        }
        // If semaphore is never released, the loop above would hang
    }

    // ── buildHostConfig ───────────────────────────────────────────────────────

    @Test
    void buildHostConfig_networkIsNone() {
        var cfg = service.buildHostConfig(Path.of("/tmp/test"));
        assertThat(cfg.getNetworkMode()).isEqualTo("none");
    }

    @Test
    void buildHostConfig_memoryMatchesProperties() {
        var cfg = service.buildHostConfig(Path.of("/tmp/test"));
        assertThat(cfg.getMemory()).isEqualTo(PROPS.memoryBytes());
        assertThat(cfg.getMemorySwap()).isEqualTo(PROPS.memoryBytes());
    }

    @Test
    void buildHostConfig_rootfsIsReadOnly() {
        var cfg = service.buildHostConfig(Path.of("/tmp/test"));
        assertThat(cfg.getReadonlyRootfs()).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static ExecutionRequest request(String solutionCode, String testCode) {
        return new ExecutionRequest(TASK_ID, solutionCode, testCode, "TaskTest");
    }
}
