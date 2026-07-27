package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.HintRequest;
import com.javaacademy.platform.ai.util.HintLeakDetector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SocraticMentorService {

    static final int MAX_HINT_TOKENS = 512;
    static final int MAX_CODE_CHARS = 4096;
    static final int MAX_ERROR_CHARS = 2048;

    static final String CANNED_HINT = "Let's take a step back. What does the error message tell you about "
            + "what the compiler or test is expecting? Try reading the error line by line — "
            + "what is the first thing that surprises you about it?";

    private final LlmClient primaryClient;
    private final LlmClient fallbackClient;
    private final String systemPrompt;

    @Autowired
    public SocraticMentorService(
            @Qualifier("geminiLlmClient") LlmClient primaryClient,
            @Qualifier("anthropicLlmClient") LlmClient fallbackClient,
            @Value("classpath:prompts/socratic-mentor-system.txt") Resource systemPromptResource)
            throws IOException {
        this.primaryClient = primaryClient;
        this.fallbackClient = fallbackClient;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Package-private constructor for unit tests — injects the system prompt directly. */
    SocraticMentorService(LlmClient primaryClient, String systemPrompt) {
        this.primaryClient = primaryClient;
        this.fallbackClient = primaryClient;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Generates a Socratic hint grounded in the task description, student's code, and the
     * compiler/test error output from their last failed submission.
     *
     * <p>Applies an anti-leak guard: if the response appears to contain solution code (code
     * fence with more than {@link HintLeakDetector#MAX_SUBSTANTIVE_LINES} substantive lines),
     * it retries once with a stricter instruction. If the retry also leaks, it returns a
     * safe {@link #CANNED_HINT} instead.
     */
    public String generateHint(HintRequest hintRequest) {
        String userPrompt = buildUserPrompt(hintRequest);
        log.debug(
                "Requesting Socratic hint for task '{}' with {} chars of student source",
                hintRequest.taskTitle(),
                hintRequest.studentSource() != null
                        ? hintRequest.studentSource().length()
                        : 0);

        String firstHint = callLlm(userPrompt);

        if (!HintLeakDetector.containsSolutionCode(firstHint)) {
            return firstHint;
        }

        log.warn(
                "Hint for task '{}' appears to contain solution code — retrying with stricter instruction",
                hintRequest.taskTitle());
        String stricterPrompt = appendLeakWarning(userPrompt, firstHint);
        String retryHint = callLlm(stricterPrompt);

        if (!HintLeakDetector.containsSolutionCode(retryHint)) {
            return retryHint;
        }

        log.error(
                "Hint retry for task '{}' still contains solution code — returning canned hint",
                hintRequest.taskTitle());
        return CANNED_HINT;
    }

    private String callLlm(String userPrompt) {
        LlmRequest request = new LlmRequest(null, systemPrompt, userPrompt, MAX_HINT_TOKENS);
        try {
            LlmResponse response = primaryClient.complete(request);
            return response.content();
        } catch (LlmException primaryException) {
            log.warn(
                    "Primary LLM (Gemini) failed for hint, falling back to Anthropic: {}",
                    primaryException.getMessage());
            LlmResponse response = fallbackClient.complete(request);
            return response.content();
        }
    }

    private static String appendLeakWarning(String originalPrompt, String leakyHint) {
        return originalPrompt + "\n\n"
                + "CRITICAL VIOLATION: Your previous response contained what appears to be "
                + "solution code inside a code block with multiple lines of Java logic:\n\n"
                + leakyHint + "\n\n"
                + "Rewrite your response with ONLY Socratic questions. "
                + "No code blocks. No method implementations. No return statements. "
                + "Only 1-2 guiding questions that help the student think, not copy.";
    }

    private String buildUserPrompt(HintRequest hintRequest) {
        String truncatedSource = truncate(hintRequest.studentSource(), MAX_CODE_CHARS);
        String truncatedError = truncate(hintRequest.errorOutput(), MAX_ERROR_CHARS);

        return "<TASK_DESCRIPTION>\n"
                + "Title: " + hintRequest.taskTitle() + "\n\n"
                + hintRequest.taskDescription() + "\n"
                + "</TASK_DESCRIPTION>\n\n"
                + "<STUDENT_CODE>\n"
                + truncatedSource + "\n"
                + "</STUDENT_CODE>\n\n"
                + "<ERROR_OUTPUT>\n"
                + (truncatedError != null ? truncatedError : "(no error output available)") + "\n"
                + "</ERROR_OUTPUT>";
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "\n[truncated]";
    }
}
