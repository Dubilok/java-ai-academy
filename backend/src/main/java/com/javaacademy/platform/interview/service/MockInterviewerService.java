package com.javaacademy.platform.interview.service;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * A4 Mock Interviewer: evaluates a completed interview session using Claude and returns
 * a structured 10-dimension evaluation report.
 */
@Slf4j
@Service
public final class MockInterviewerService {

    static final int MAX_EVALUATION_TOKENS = 1500;
    static final int MAX_ANSWER_CHARS = 1000;

    private final LlmClient llmClient;
    private final EvaluationReportParser reportParser;
    private final String systemPrompt;

    @Autowired
    public MockInterviewerService(
            @Qualifier("anthropicLlmClient") LlmClient llmClient,
            EvaluationReportParser reportParser,
            @Value("classpath:prompts/mock-interviewer-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.reportParser = reportParser;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Package-private constructor for unit tests. */
    MockInterviewerService(LlmClient llmClient, EvaluationReportParser reportParser, String systemPrompt) {
        this.llmClient = llmClient;
        this.reportParser = reportParser;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Evaluates the Q&A transcript from a completed session and returns a validated 10-dimension
     * {@link EvaluationReport}.
     *
     * @param answers    all answers from the session, in chronological order
     * @param sessionId  the session being evaluated
     * @param technology the technology topic of the session
     */
    public EvaluationReport evaluate(List<InterviewAnswer> answers, UUID sessionId, String technology) {
        String transcript = buildTranscript(answers, technology);
        log.debug("Requesting evaluation for session {} ({} answers)", sessionId, answers.size());

        LlmRequest request = new LlmRequest(null, systemPrompt, transcript, MAX_EVALUATION_TOKENS);
        LlmResponse response = llmClient.complete(request);

        return reportParser.parse(response.content(), sessionId, technology);
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
                String answerText = answer.getAnswerText();
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
