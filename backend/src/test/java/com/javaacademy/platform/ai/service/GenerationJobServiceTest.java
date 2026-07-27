package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.dto.GenerateCourseRequest;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.dto.GeneratedLecture;
import com.javaacademy.platform.ai.dto.GeneratedTask;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.catalog.enums.Difficulty;
import com.javaacademy.platform.catalog.service.ContentImportService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerationJobServiceTest {

    ContentArchitectService contentArchitectService;
    ContentImportService contentImportService;
    GenerationJobService service;

    @BeforeEach
    void setUp() {
        contentArchitectService = mock(ContentArchitectService.class);
        contentImportService = mock(ContentImportService.class);
        service = new GenerationJobService(contentArchitectService, contentImportService);
    }

    @Test
    void startJob_returnsJobId_andJobIsRunningOrSucceeded() {
        when(contentArchitectService.generateForTopic(any())).thenReturn(sampleContent());
        when(contentImportService.importGenerated(any(), any())).thenReturn(UUID.randomUUID());

        UUID jobId = service.startJob(new GenerateCourseRequest("Java Records", null, null, null, null));

        assertThat(jobId).isNotNull();
        // job exists (may already have completed if virtual thread is fast)
        assertThat(service.getJob(jobId)).isPresent();
    }

    @Test
    void getJob_unknownId_returnsEmpty() {
        assertThat(service.getJob(UUID.randomUUID())).isEmpty();
    }

    @Test
    void startJob_successPath_jobEventuallySucceeds() throws InterruptedException {
        UUID expectedCourseId = UUID.randomUUID();
        when(contentArchitectService.generateForTopic(eq("Java Records"))).thenReturn(sampleContent());
        when(contentImportService.importGenerated(eq("Java Records"), any())).thenReturn(expectedCourseId);

        UUID jobId = service.startJob(new GenerateCourseRequest("Java Records", null, null, null, null));
        awaitTerminal(service, jobId);

        Optional<GenerationJobResponse> response = service.getJob(jobId);
        assertThat(response).isPresent();
        assertThat(response.get().status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(response.get().courseId()).isEqualTo(expectedCourseId);
        assertThat(response.get().errorMessage()).isNull();
    }

    @Test
    void startJob_generationFailure_jobEventuallyFails() throws InterruptedException {
        when(contentArchitectService.generateForTopic(any()))
                .thenThrow(new com.javaacademy.platform.ai.client.LlmException("exhausted"));

        UUID jobId = service.startJob(new GenerateCourseRequest("Java Records", null, null, null, null));
        awaitTerminal(service, jobId);

        Optional<GenerationJobResponse> response = service.getJob(jobId);
        assertThat(response).isPresent();
        assertThat(response.get().status()).isEqualTo(JobStatus.FAILED);
        assertThat(response.get().errorMessage()).contains("exhausted");
        assertThat(response.get().courseId()).isNull();
    }

    @Test
    void startJob_multipleConcurrentJobs_eachTrackedIndependently() {
        UUID courseId1 = UUID.randomUUID();
        UUID courseId2 = UUID.randomUUID();
        when(contentArchitectService.generateForTopic(eq("Java Records"))).thenReturn(sampleContent());
        when(contentArchitectService.generateForTopic(eq("Spring Boot"))).thenReturn(sampleContent());
        when(contentImportService.importGenerated(eq("Java Records"), any())).thenReturn(courseId1);
        when(contentImportService.importGenerated(eq("Spring Boot"), any())).thenReturn(courseId2);

        UUID jobId1 = service.startJob(new GenerateCourseRequest("Java Records", null, null, null, null));
        UUID jobId2 = service.startJob(new GenerateCourseRequest("Spring Boot", null, null, null, null));

        assertThat(jobId1).isNotEqualTo(jobId2);
        assertThat(service.getJob(jobId1)).isPresent();
        assertThat(service.getJob(jobId2)).isPresent();
    }

    private static void awaitTerminal(GenerationJobService generationJobService, UUID jobId)
            throws InterruptedException {
        for (int iteration = 0; iteration < 100; iteration++) {
            Optional<GenerationJobResponse> response = generationJobService.getJob(jobId);
            if (response.isPresent() && response.get().status() != JobStatus.RUNNING) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private static GeneratedContent sampleContent() {
        GeneratedLecture lecture = new GeneratedLecture("Java Records", "A".repeat(200));
        GeneratedTask task = new GeneratedTask("Create a Point", "B".repeat(60), Difficulty.EASY, 100);
        return new GeneratedContent(
                lecture,
                task,
                "public class Solution {}",
                "public class Solution {}",
                "import org.junit.jupiter.api.Test;\npublic class TaskTest { @Test void t() {} }");
    }
}
