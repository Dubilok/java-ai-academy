package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Asserts that every §6.2 sandbox hardening flag is applied to the container config.
 * Each flag has its own test so a regression is immediately identifiable.
 */
class SandboxHardeningTest {

    static final SandboxProperties PROPS = new SandboxProperties("java-ai-academy/runner:21", 134_217_728L, 5, 4);
    static final Path WORK_DIR = Path.of("/tmp/sandbox-test");

    DockerClient dockerClient;
    DockerCodeExecutionService service;
    HostConfig hostConfig;

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
        hostConfig = service.buildHostConfig(WORK_DIR);

        createCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("c999");
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

    // ── Network isolation ─────────────────────────────────────────────────────

    @Test
    void hostConfig_networkIsNone() {
        assertThat(hostConfig.getNetworkMode()).isEqualTo("none");
    }

    // ── Memory limits ─────────────────────────────────────────────────────────

    @Test
    void hostConfig_memoryLimitIs128MB() {
        assertThat(hostConfig.getMemory()).isEqualTo(134_217_728L);
    }

    @Test
    void hostConfig_memorySwapEqualsMemoryLimitDisablingSwap() {
        assertThat(hostConfig.getMemorySwap()).isEqualTo(hostConfig.getMemory());
    }

    // ── CPU limits ────────────────────────────────────────────────────────────

    @Test
    void hostConfig_cpuQuotaIs50PctOfPeriod() {
        long period = hostConfig.getCpuPeriod();
        long quota = hostConfig.getCpuQuota();
        assertThat(period).isEqualTo(100_000L);
        assertThat(quota).isEqualTo(50_000L);
        assertThat((double) quota / period).isEqualTo(0.5);
    }

    // ── PID limit ─────────────────────────────────────────────────────────────

    @Test
    void hostConfig_pidsLimitIs64() {
        assertThat(hostConfig.getPidsLimit()).isEqualTo(64L);
    }

    // ── Read-only root filesystem ─────────────────────────────────────────────

    @Test
    void hostConfig_rootfsIsReadOnly() {
        assertThat(hostConfig.getReadonlyRootfs()).isTrue();
    }

    // ── Capability drop ───────────────────────────────────────────────────────

    @Test
    void hostConfig_dropsAllCapabilities() {
        assertThat(hostConfig.getCapDrop()).contains(Capability.ALL);
    }

    // ── Privilege escalation prevention ──────────────────────────────────────

    @Test
    void hostConfig_noNewPrivilegesIsSet() {
        assertThat(hostConfig.getSecurityOpts()).contains("no-new-privileges:true");
    }

    // ── tmpfs for scratch space ───────────────────────────────────────────────

    @Test
    void hostConfig_tmpfsMountExists() {
        assertThat(hostConfig.getTmpFs()).containsKey("/tmp");
    }

    @Test
    void hostConfig_tmpfsMountIsNoexec() {
        String tmpfsOptions = hostConfig.getTmpFs().get("/tmp");
        assertThat(tmpfsOptions).contains("noexec");
    }

    // ── Workdir bind mount ────────────────────────────────────────────────────

    @Test
    void hostConfig_workDirIsBoundToContainerWorkDir() {
        assertThat(hostConfig.getBinds())
                .anySatisfy(bind -> assertThat(bind.getVolume().getPath())
                        .isEqualTo(DockerCodeExecutionService.CONTAINER_WORK_DIR));
    }

    // ── Non-root user ─────────────────────────────────────────────────────────

    @Test
    void createContainer_runsAsNonRootUser() {
        service.createContainer(WORK_DIR, "TaskTest");
        verify(createCmd).withUser("1000:0");
    }

    // ── Log cap ───────────────────────────────────────────────────────────────

    @Test
    void logCapConstant_is64KB() {
        assertThat(DockerCodeExecutionService.LOG_CAP_BYTES).isEqualTo(65_536L);
    }

    // ── Timeout from properties ───────────────────────────────────────────────

    @Test
    void sandboxProperties_timeoutMatchesSpec() {
        assertThat(PROPS.timeoutSeconds()).isEqualTo(5);
    }
}
