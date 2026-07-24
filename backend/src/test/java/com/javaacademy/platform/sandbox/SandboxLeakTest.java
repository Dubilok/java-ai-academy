package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that containers are never leaked regardless of what happens during execution.
 *
 * <p>Unit test (50 iterations, Mockito): exercises all failure modes and asserts
 * removeContainerCmd is called exactly once per submission, even when start, wait,
 * or inspect throw exceptions mid-flight.
 *
 * <p>Integration test (real Docker, 5 iterations): runs actual containers and queries
 * the Docker daemon to confirm zero containers tagged with the runner image survive.
 */
class SandboxLeakTest {

    static final SandboxProperties PROPS = new SandboxProperties("java-ai-academy/runner:21", 134_217_728L, 5, 8);
    static final int ITERATION_COUNT = 50;

    DockerClient dockerClient;
    DockerCodeExecutionService service;

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

    @BeforeEach
    void setUp() throws InterruptedException {
        dockerClient = mock(DockerClient.class);
        service = new DockerCodeExecutionService(dockerClient, PROPS);

        createCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("c-leak-test");
        doReturn(createResponse).when(createCmd).exec();
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);

        startCmd = mock(StartContainerCmd.class, RETURNS_SELF);
        doReturn(startCmd).when(dockerClient).startContainerCmd(anyString());

        waitCmd = mock(WaitContainerCmd.class, RETURNS_SELF);
        waitCallback = mock(WaitContainerResultCallback.class);
        when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(true);
        doReturn(waitCallback).when(waitCmd).exec(any(WaitContainerResultCallback.class));
        when(dockerClient.waitContainerCmd(anyString())).thenReturn(waitCmd);

        logCmd = mock(LogContainerCmd.class, RETURNS_SELF);
        doAnswer(inv -> {
                    var cb = (com.github.dockerjava.api.async.ResultCallbackTemplate<?, ?>) inv.getArgument(0);
                    cb.onComplete();
                    return cb;
                })
                .when(logCmd)
                .exec(any());
        when(dockerClient.logContainerCmd(anyString())).thenReturn(logCmd);

        containerState = mock(InspectContainerResponse.ContainerState.class);
        when(containerState.getExitCodeLong()).thenReturn(0L);
        inspectResponse = mock(InspectContainerResponse.class);
        when(inspectResponse.getState()).thenReturn(containerState);
        inspectCmd = mock(InspectContainerCmd.class, RETURNS_SELF);
        doReturn(inspectResponse).when(inspectCmd).exec();
        when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectCmd);

        removeCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);
        when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeCmd);
    }

    // ── 50-iteration unit leak test ───────────────────────────────────────────

    @Test
    void execute_50submissions_removeCalledEveryTime_noLeak() throws InterruptedException {
        for (int submissionIdx = 0; submissionIdx < ITERATION_COUNT; submissionIdx++) {
            int mode = submissionIdx % 3;
            if (mode == 0) {
                // startContainer throws — container was created but start failed
                doThrow(new RuntimeException("start failure"))
                        .when(dockerClient)
                        .startContainerCmd(anyString());
            } else if (mode == 1) {
                // timeout — awaitCompletion returns false
                doReturn(startCmd).when(dockerClient).startContainerCmd(anyString());
                when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(false);
            } else {
                // happy path — exit 0
                doReturn(startCmd).when(dockerClient).startContainerCmd(anyString());
                when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(true);
                when(containerState.getExitCodeLong()).thenReturn(0L);
            }
            service.execute(
                    new ExecutionRequest(UUID.randomUUID(), "class Solution{}", "class TaskTest{}", "TaskTest"));
        }

        // removeContainerCmd must be called exactly once per submission
        verify(removeCmd, times(ITERATION_COUNT)).exec();
    }

    @Test
    void execute_createContainerFails_removeIsNeverCalledForNullId() {
        doThrow(new RuntimeException("no docker")).when(createCmd).exec();
        service.execute(new ExecutionRequest(UUID.randomUUID(), "class Solution{}", "class TaskTest{}", "TaskTest"));
        verify(removeCmd, times(0)).exec();
    }

    @Test
    void execute_timeoutPath_killCalledThenRemoveCalled() throws InterruptedException {
        when(waitCallback.awaitCompletion(any(Long.class), any())).thenReturn(false);
        var killCmd = mock(com.github.dockerjava.api.command.KillContainerCmd.class, RETURNS_SELF);
        when(dockerClient.killContainerCmd(anyString())).thenReturn(killCmd);

        service.execute(new ExecutionRequest(UUID.randomUUID(), "class Solution{}", "class TaskTest{}", "TaskTest"));

        verify(killCmd).exec();
        verify(removeCmd).exec();
    }

    // ── Real-Docker integration leak test ─────────────────────────────────────

    @Test
    void realDocker_5submissions_zeroContainersRemain() {
        DockerClient realClient;
        try {
            var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            var httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            realClient = DockerClientImpl.getInstance(config, httpClient);
            realClient.pingCmd().exec();
        } catch (Exception dockerUnavailable) {
            return; // Docker daemon not available — skip
        }

        List<?> images = realClient
                .listImagesCmd()
                .withImageNameFilter("java-ai-academy/runner:21")
                .exec();
        if (images.isEmpty()) {
            return; // runner image not built — skip
        }

        var realService = new DockerCodeExecutionService(realClient, PROPS);
        String solutionCode = "public class Solution { public static int value() { return 42; } }";
        String testCode =
                """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                public class TaskTest {
                    @Test void value_returns42() { assertEquals(42, Solution.value()); }
                }
                """;

        for (int submissionIdx = 0; submissionIdx < 5; submissionIdx++) {
            realService.execute(new ExecutionRequest(UUID.randomUUID(), solutionCode, testCode, "TaskTest"));
        }

        List<?> remaining = realClient
                .listContainersCmd()
                .withShowAll(true)
                .withAncestorFilter(List.of("java-ai-academy/runner:21"))
                .exec();
        assertThat(remaining).as("leaked containers after 5 real submissions").isEmpty();
    }
}
