package com.javaacademy.platform.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.rag.LectureIndexingService;
import com.javaacademy.platform.ai.rag.VectorMatch;
import com.javaacademy.platform.interview.dto.EvaluationDimension;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.dto.RagCitation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockInterviewerServiceTest {

    static final UUID SESSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID LECTURE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    static final String TECHNOLOGY = "Java";
    static final String SYSTEM_PROMPT = "You are a mock interviewer.";
    static final String FAKE_LLM_JSON = "{\"dimensions\":[]}";

    LlmClient mockLlmClient;
    EvaluationReportParser mockParser;
    LectureIndexingService mockRagService;

    @BeforeEach
    void setUp() {
        mockLlmClient = mock(LlmClient.class);
        mockParser = mock(EvaluationReportParser.class);
        mockRagService = mock(LectureIndexingService.class);

        when(mockLlmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse(FAKE_LLM_JSON, 10, 200));
        when(mockParser.parse(anyString(), any(UUID.class), anyString())).thenReturn(baseReport());
    }

    @Test
    void evaluate_withoutRag_citationsAreNull() {
        MockInterviewerService service = new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT);

        EvaluationReport report = service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        assertThat(report.citations()).isNull();
        verify(mockLlmClient).complete(any(LlmRequest.class));
    }

    @Test
    void evaluate_withRag_attachesMatchesAsCitations() {
        VectorMatch match = new VectorMatch(LECTURE_ID, 0, "Java records are immutable.", 0.92);
        when(mockRagService.search(eq(TECHNOLOGY), eq(MockInterviewerService.RAG_TOP_K)))
                .thenReturn(List.of(match));

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);

        EvaluationReport report = service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        assertThat(report.citations()).hasSize(1);
        RagCitation citation = report.citations().get(0);
        assertThat(citation.lectureId()).isEqualTo(LECTURE_ID);
        assertThat(citation.chunkIndex()).isEqualTo(0);
        assertThat(citation.snippet()).isEqualTo("Java records are immutable.");
    }

    @Test
    void evaluate_ragReturnsEmpty_citationsAbsent() {
        when(mockRagService.search(anyString(), anyInt())).thenReturn(List.of());

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);

        EvaluationReport report = service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        assertThat(report.citations()).isNull();
    }

    @Test
    void evaluate_ragSearchThrows_proceedsWithoutCitations() {
        when(mockRagService.search(anyString(), anyInt())).thenThrow(new RuntimeException("Vector DB unavailable"));

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);

        EvaluationReport report = service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        assertThat(report.citations()).isNull();
        verify(mockLlmClient).complete(any(LlmRequest.class));
    }

    @Test
    void evaluate_withRag_includesReferenceMaterialInUserMessage() {
        VectorMatch match = new VectorMatch(LECTURE_ID, 2, "String is immutable in Java.", 0.88);
        when(mockRagService.search(eq(TECHNOLOGY), anyInt())).thenReturn(List.of(match));

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);
        service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        var captor = org.mockito.ArgumentCaptor.forClass(LlmRequest.class);
        verify(mockLlmClient).complete(captor.capture());

        String userMessage = captor.getValue().userPrompt();
        assertThat(userMessage).contains("<REFERENCE_MATERIAL");
        assertThat(userMessage).contains("lecture_id=" + LECTURE_ID);
        assertThat(userMessage).contains("String is immutable in Java.");
        assertThat(userMessage).contains("<TRANSCRIPT");
    }

    @Test
    void evaluate_withoutRag_noReferenceMaterialInUserMessage() {
        MockInterviewerService service = new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT);
        service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        var captor = org.mockito.ArgumentCaptor.forClass(LlmRequest.class);
        verify(mockLlmClient).complete(captor.capture());

        String userMessage = captor.getValue().userPrompt();
        assertThat(userMessage).doesNotContain("<REFERENCE_MATERIAL");
        assertThat(userMessage).contains("<TRANSCRIPT");
    }

    @Test
    void evaluate_snippetTruncated_whenMatchTextExceedsLimit() {
        String longText = "A".repeat(500);
        VectorMatch match = new VectorMatch(LECTURE_ID, 0, longText, 0.9);
        when(mockRagService.search(anyString(), anyInt())).thenReturn(List.of(match));

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);
        EvaluationReport report = service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        assertThat(report.citations()).hasSize(1);
        assertThat(report.citations().get(0).snippet())
                .hasSizeLessThanOrEqualTo(MockInterviewerService.SNIPPET_MAX_CHARS + "[truncated]".length());
    }

    @Test
    void evaluate_emptyAnswers_transcriptContainsNoAnswersMessage() {
        MockInterviewerService service = new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT);
        service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        var captor = org.mockito.ArgumentCaptor.forClass(LlmRequest.class);
        verify(mockLlmClient).complete(captor.capture());

        assertThat(captor.getValue().userPrompt()).contains("No answers recorded");
    }

    @Test
    void evaluate_withRag_ragSearchedWithCorrectTechnology() {
        when(mockRagService.search(anyString(), anyInt())).thenReturn(List.of());

        MockInterviewerService service =
                new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT, mockRagService);
        service.evaluate(List.of(), SESSION_ID, "Spring Boot");

        verify(mockRagService).search(eq("Spring Boot"), eq(MockInterviewerService.RAG_TOP_K));
    }

    @Test
    void evaluate_withoutRag_ragNeverQueried() {
        MockInterviewerService service = new MockInterviewerService(mockLlmClient, mockParser, SYSTEM_PROMPT);
        service.evaluate(List.of(), SESSION_ID, TECHNOLOGY);

        verify(mockRagService, never()).search(anyString(), anyInt());
    }

    private static EvaluationReport baseReport() {
        List<EvaluationDimension> dims = List.of(
                new EvaluationDimension("technicalAccuracy", 80, "Good."),
                new EvaluationDimension("depthOfKnowledge", 80, "Good."),
                new EvaluationDimension("practicalApplication", 80, "Good."),
                new EvaluationDimension("communicationClarity", 80, "Good."),
                new EvaluationDimension("breadthOfCoverage", 80, "Good."),
                new EvaluationDimension("exampleQuality", 80, "Good."),
                new EvaluationDimension("problemSolvingApproach", 80, "Good."),
                new EvaluationDimension("edgeCaseAwareness", 80, "Good."),
                new EvaluationDimension("modernJavaAwareness", 80, "Good."),
                new EvaluationDimension("learningPotential", 80, "Good."));
        return new EvaluationReport(SESSION_ID, TECHNOLOGY, 80, dims, Instant.parse("2026-07-25T12:00:00Z"), null);
    }
}
