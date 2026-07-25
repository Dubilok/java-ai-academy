package com.javaacademy.platform.interview.service;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.rag.LectureIndexingService;
import com.javaacademy.platform.ai.rag.VectorMatch;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.dto.RagCitation;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * A4 Mock Interviewer: evaluates a completed interview session using Claude and returns
 * a structured 10-dimension evaluation report.
 *
 * <p>When a {@link LectureIndexingService} is present (Bedrock provider), relevant lecture chunks
 * are retrieved and injected as reference material before the transcript. The retrieved chunks are
 * returned as {@link RagCitation} objects on the report.
 */
@Slf4j
@Service
public final class MockInterviewerService {

    static final int MAX_EVALUATION_TOKENS = 1500;
    static final int MAX_ANSWER_CHARS = 1000;
    static final int RAG_TOP_K = 3;
    static final int SNIPPET_MAX_CHARS = 300;

    private final LlmClient llmClient;
    private final EvaluationReportParser reportParser;
    private final String systemPrompt;
    private final Optional<LectureIndexingService> ragService;

    @Autowired
    public MockInterviewerService(
            @Qualifier("anthropicLlmClient") LlmClient llmClient,
            EvaluationReportParser reportParser,
            @Value("classpath:prompts/mock-interviewer-system.txt") Resource systemPromptResource,
            Optional<LectureIndexingService> ragService)
            throws IOException {
        this.llmClient = llmClient;
        this.reportParser = reportParser;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.ragService = ragService;
    }

    /** Package-private constructor for unit tests — RAG disabled. */
    MockInterviewerService(LlmClient llmClient, EvaluationReportParser reportParser, String systemPrompt) {
        this.llmClient = llmClient;
        this.reportParser = reportParser;
        this.systemPrompt = systemPrompt;
        this.ragService = Optional.empty();
    }

    /** Package-private constructor for unit tests — RAG explicitly provided. */
    MockInterviewerService(
            LlmClient llmClient,
            EvaluationReportParser reportParser,
            String systemPrompt,
            LectureIndexingService ragService) {
        this.llmClient = llmClient;
        this.reportParser = reportParser;
        this.systemPrompt = systemPrompt;
        this.ragService = Optional.of(ragService);
    }

    /**
     * Evaluates the Q&A transcript from a completed session and returns a validated 10-dimension
     * {@link EvaluationReport}. When RAG is available, the report includes citations from the lecture
     * corpus.
     *
     * @param answers    all answers from the session, in chronological order
     * @param sessionId  the session being evaluated
     * @param technology the technology topic of the session
     */
    public EvaluationReport evaluate(List<InterviewAnswer> answers, UUID sessionId, String technology) {
        List<VectorMatch> matches = retrieveContext(technology);
        List<RagCitation> citations = toCitations(matches);

        String userMessage = buildUserMessage(answers, technology, matches);
        log.debug(
                "Requesting evaluation for session {} ({} answers, {} RAG chunks)",
                sessionId,
                answers.size(),
                matches.size());

        LlmRequest request = new LlmRequest(null, systemPrompt, userMessage, MAX_EVALUATION_TOKENS);
        LlmResponse response = llmClient.complete(request);

        EvaluationReport baseReport = reportParser.parse(response.content(), sessionId, technology);
        return withCitations(baseReport, citations);
    }

    private List<VectorMatch> retrieveContext(String technology) {
        if (ragService.isEmpty()) {
            return List.of();
        }
        try {
            return ragService.get().search(technology, RAG_TOP_K);
        } catch (Exception ragException) {
            log.warn(
                    "RAG search failed for technology '{}', proceeding without context: {}",
                    technology,
                    ragException.getMessage());
            return List.of();
        }
    }

    private static List<RagCitation> toCitations(List<VectorMatch> matches) {
        return matches.stream()
                .map(match -> new RagCitation(
                        match.lectureId(), match.chunkIndex(), truncate(match.chunkText(), SNIPPET_MAX_CHARS)))
                .toList();
    }

    private static EvaluationReport withCitations(EvaluationReport report, List<RagCitation> citations) {
        if (citations.isEmpty()) {
            return report;
        }
        return new EvaluationReport(
                report.sessionId(),
                report.technology(),
                report.overallScore(),
                report.dimensions(),
                report.completedAt(),
                citations);
    }

    private static String buildUserMessage(
            List<InterviewAnswer> answers, String technology, List<VectorMatch> matches) {
        StringBuilder sb = new StringBuilder();

        if (!matches.isEmpty()) {
            sb.append("<REFERENCE_MATERIAL technology=\"").append(technology).append("\">\n");
            for (int matchIndex = 0; matchIndex < matches.size(); matchIndex++) {
                VectorMatch match = matches.get(matchIndex);
                sb.append("[")
                        .append(matchIndex + 1)
                        .append("] lecture_id=")
                        .append(match.lectureId())
                        .append(" chunk=")
                        .append(match.chunkIndex())
                        .append(":\n");
                sb.append(truncate(match.chunkText(), SNIPPET_MAX_CHARS)).append("\n\n");
            }
            sb.append("</REFERENCE_MATERIAL>\n\n");
        }

        sb.append(buildTranscript(answers, technology));
        return sb.toString();
    }

    private static String buildTranscript(List<InterviewAnswer> answers, String technology) {
        StringBuilder sb = new StringBuilder();
        sb.append("<TRANSCRIPT technology=\"").append(technology).append("\">\n");

        if (answers.isEmpty()) {
            sb.append("(No answers recorded — session ended before any questions were answered.)\n");
        } else {
            int answerNumber = 1;
            for (InterviewAnswer answer : answers) {
                sb.append("Q").append(answerNumber).append(": ");
                if (answer.getQuestion() != null) {
                    sb.append(answer.getQuestion().getQuestion());
                } else {
                    sb.append("(question no longer available)");
                }
                sb.append("\n");
                sb.append("A").append(answerNumber).append(": ");
                @Nullable String answerText = answer.getAnswerText();
                if (answerText == null || answerText.isBlank()) {
                    sb.append("(no answer provided)");
                } else {
                    sb.append(truncate(answerText, MAX_ANSWER_CHARS));
                }
                sb.append("\n\n");
                answerNumber++;
            }
        }

        sb.append("</TRANSCRIPT>");
        return sb.toString();
    }

    private static String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "[truncated]";
    }
}
