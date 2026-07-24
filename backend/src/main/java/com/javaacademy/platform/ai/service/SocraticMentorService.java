package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.HintRequest;
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

    private final LlmClient llmClient;
    private final String systemPrompt;

    @Autowired
    public SocraticMentorService(
            @Qualifier("geminiLlmClient") LlmClient llmClient,
            @Value("classpath:prompts/socratic-mentor-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Package-private constructor for unit tests — injects the system prompt directly. */
    SocraticMentorService(LlmClient llmClient, String systemPrompt) {
        this.llmClient = llmClient;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Generates a Socratic hint grounded in the task description, student's code, and the
     * compiler/test error output from their last failed submission.
     *
     * <p>The model is instructed to ask guiding questions — never to emit solution code.
     */
    public String generateHint(HintRequest hintRequest) {
        String userPrompt = buildUserPrompt(hintRequest);
        log.debug(
                "Requesting Socratic hint for task '{}' with {} chars of student source",
                hintRequest.taskTitle(),
                hintRequest.studentSource() != null
                        ? hintRequest.studentSource().length()
                        : 0);
        LlmRequest request = new LlmRequest(null, systemPrompt, userPrompt, MAX_HINT_TOKENS);
        LlmResponse response = llmClient.complete(request);
        return response.content();
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
