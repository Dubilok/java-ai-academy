package com.javaacademy.platform.ai.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.dto.CurriculumProposalResponse;
import com.javaacademy.platform.ai.dto.ProposeLecturesResponse;
import com.javaacademy.platform.ai.dto.ProposeModulesResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CurriculumArchitectService {

    private static final int MAX_TOKENS = 2048;

    private final LlmClient llmClient;
    private final String systemPrompt;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    @Autowired
    public CurriculumArchitectService(
            LlmClient llmClient,
            @Value("classpath:prompts/curriculum-architect-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Package-private constructor for tests. */
    CurriculumArchitectService(LlmClient llmClient, String systemPrompt) {
        this.llmClient = llmClient;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Calls the LLM to propose a full course curriculum for the given technology.
     * Returns a structured proposal with modules and lecture topics.
     *
     * @throws LlmException if the LLM call fails or the response cannot be parsed
     */
    public CurriculumProposalResponse proposeCurriculum(String technology) {
        log.info("Proposing curriculum for '{}'", technology);
        String userPrompt = "Design a complete learning path for: " + technology;
        LlmRequest request = new LlmRequest(null, systemPrompt, userPrompt, MAX_TOKENS);

        String rawContent = llmClient.complete(request).content();
        String json = extractJson(rawContent);

        try {
            CurriculumProposalResponse parsed = objectMapper.readValue(json, CurriculumProposalResponse.class);
            // Inject the input technology — the LLM only returns courseName/description/modules
            CurriculumProposalResponse proposal = new CurriculumProposalResponse(
                    technology, parsed.courseName(), parsed.description(), parsed.modules());
            validate(proposal, technology);
            log.info(
                    "Curriculum proposed for '{}': {} modules",
                    technology,
                    proposal.modules() != null ? proposal.modules().size() : 0);
            return proposal;
        } catch (IOException ioException) {
            throw new LlmException("Failed to parse curriculum proposal: " + ioException.getMessage());
        }
    }

    /**
     * Asks the LLM to propose additional modules for an existing course, avoiding duplicates.
     *
     * @throws LlmException if the LLM call fails or the response cannot be parsed
     */
    public ProposeModulesResponse proposeAdditionalModules(String technology, List<String> existingModules) {
        String existing = existingModules.isEmpty()
                ? "(none yet)"
                : existingModules.stream().map(name -> "- " + name).reduce("", (a, b) -> a + "\n" + b);
        String systemPrompt = "You are a Java curriculum designer. Respond with JSON only, no markdown fences.";
        String userPrompt = "The course '" + technology + "' already has these modules:\n" + existing
                + "\n\nSuggest 3-5 new, distinct modules that complement the existing ones."
                + " Do not repeat any existing module name."
                + " Return JSON: {\"modules\": [{\"moduleName\": \"...\", \"lectureTopics\": [\"...\", \"...\"]}]}";

        String raw = llmClient
                .complete(new LlmRequest(null, systemPrompt, userPrompt, 1024))
                .content();
        try {
            ProposeModulesResponse response = objectMapper.readValue(extractJson(raw), ProposeModulesResponse.class);
            if (response.modules() == null || response.modules().isEmpty()) {
                throw new LlmException("No additional modules returned for '" + technology + "'");
            }
            log.info(
                    "Proposed {} additional modules for '{}'",
                    response.modules().size(),
                    technology);
            return response;
        } catch (IOException ioException) {
            throw new LlmException("Failed to parse additional modules: " + ioException.getMessage());
        }
    }

    /**
     * Asks the LLM to propose additional lecture topics for a specific module.
     *
     * @throws LlmException if the LLM call fails or the response cannot be parsed
     */
    public ProposeLecturesResponse proposeAdditionalLectures(
            String technology, String moduleName, List<String> existingLectures) {
        String existing = existingLectures.isEmpty()
                ? "(none yet)"
                : existingLectures.stream().map(t -> "- " + t).reduce("", (a, b) -> a + "\n" + b);
        String systemPrompt = "You are a Java curriculum designer. Respond with JSON only, no markdown fences.";
        String userPrompt = "The module '" + moduleName + "' in the '" + technology
                + "' course already covers:\n" + existing
                + "\n\nSuggest 3-5 additional lecture topics that complement the existing ones."
                + " Do not repeat existing topics."
                + " Return JSON: {\"lectureTopics\": [\"...\", \"...\"]}";

        String raw = llmClient
                .complete(new LlmRequest(null, systemPrompt, userPrompt, 512))
                .content();
        try {
            ProposeLecturesResponse response = objectMapper.readValue(extractJson(raw), ProposeLecturesResponse.class);
            if (response.lectureTopics() == null || response.lectureTopics().isEmpty()) {
                throw new LlmException("No additional lectures returned for module '" + moduleName + "'");
            }
            log.info(
                    "Proposed {} additional lectures for module '{}' in '{}'",
                    response.lectureTopics().size(),
                    moduleName,
                    technology);
            return response;
        } catch (IOException ioException) {
            throw new LlmException("Failed to parse additional lectures: " + ioException.getMessage());
        }
    }

    private void validate(CurriculumProposalResponse proposal, String technology) {
        if (proposal.modules() == null || proposal.modules().isEmpty()) {
            throw new LlmException("Curriculum proposal for '" + technology + "' contains no modules");
        }
        for (var module : proposal.modules()) {
            if (module.lectureTopics() == null || module.lectureTopics().isEmpty()) {
                throw new LlmException("Module '" + module.moduleName() + "' has no lecture topics");
            }
        }
    }

    private static String extractJson(String raw) {
        String trimmed = raw.strip();
        // Strip markdown fences if the model wrapped the JSON anyway
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }
}
