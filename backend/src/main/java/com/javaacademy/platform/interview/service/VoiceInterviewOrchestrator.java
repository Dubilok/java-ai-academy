package com.javaacademy.platform.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.AnthropicProperties;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.interview.dto.TurnAssessment;
import com.javaacademy.platform.interview.dto.TurnResult;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import com.javaacademy.platform.interview.entity.InterviewSession;
import com.javaacademy.platform.interview.entity.VoiceInterviewQuestion;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.VoiceInterviewQuestionRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VoiceInterviewOrchestrator {

    static final int MAX_TOKENS = 1024;
    static final int MAX_TRANSCRIPT_CHARS = 2000;
    static final int MAX_HISTORY_CHARS = 4000;

    private final LlmClient llmClient;
    private final AnthropicProperties anthropicProperties;
    private final VoiceInterviewQuestionRepository voiceQuestionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;

    @Autowired
    public VoiceInterviewOrchestrator(
            @Qualifier("anthropicLlmClient") LlmClient llmClient,
            AnthropicProperties anthropicProperties,
            VoiceInterviewQuestionRepository voiceQuestionRepository,
            InterviewAnswerRepository answerRepository,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/voice-interviewer-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.anthropicProperties = anthropicProperties;
        this.voiceQuestionRepository = voiceQuestionRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Package-private constructor for unit tests — no classpath loading. */
    VoiceInterviewOrchestrator(
            LlmClient llmClient,
            AnthropicProperties anthropicProperties,
            VoiceInterviewQuestionRepository voiceQuestionRepository,
            InterviewAnswerRepository answerRepository,
            ObjectMapper objectMapper,
            String systemPrompt) {
        this.llmClient = llmClient;
        this.anthropicProperties = anthropicProperties;
        this.voiceQuestionRepository = voiceQuestionRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Generates the opening question for a VOICE session (turn index 0). Persists the generated
     * question to the database and sets the session's initial topic map.
     */
    @Transactional
    public TurnResult generateOpeningQuestion(InterviewSession session, int maxTurns) {
        String userMessage = buildOpeningPrompt(session.getTechnology(), maxTurns);
        TurnResult result = callAndParse(userMessage, session, 0);

        persistVoiceQuestion(session, 0, result.question());
        updateTopicMap(session, result.topicMap());

        log.info("Generated opening question for session {} technology={}", session.getId(), session.getTechnology());
        return result;
    }

    /**
     * Generates the next question after a candidate's voice answer. Persists the generated question
     * and updates the session's topic map.
     */
    @Transactional
    public TurnResult generateNextQuestion(InterviewSession session, String transcript, int turnIndex) {
        List<VoiceInterviewQuestion> history = voiceQuestionRepository.findBySessionIdOrderByTurnIndex(session.getId());
        List<InterviewAnswer> answers =
                answerRepository.findBySessionIdWithVoiceQuestionOrderByCreatedAt(session.getId());

        String userMessage = buildTurnPrompt(session.getTechnology(), history, answers, transcript, turnIndex);
        TurnResult result = callAndParse(userMessage, session, turnIndex);

        persistVoiceQuestion(session, turnIndex, result.question());
        updateTopicMap(session, result.topicMap());

        log.info(
                "Generated turn {} question for session {} isFinalTurn={}",
                turnIndex,
                session.getId(),
                result.isFinalTurn());
        return result;
    }

    private TurnResult callAndParse(String userMessage, InterviewSession session, int turnIndex) {
        String model = anthropicProperties.effectiveGenerationModel();
        LlmRequest request = new LlmRequest(model, systemPrompt, userMessage, MAX_TOKENS);
        String content = llmClient.complete(request).content();
        try {
            return parseTurnResult(content);
        } catch (LlmException llmException) {
            throw llmException;
        } catch (Exception parseException) {
            throw new LlmException("Failed to parse voice turn result for session " + session.getId() + " turn "
                    + turnIndex + ": " + parseException.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TurnResult parseTurnResult(String json) throws Exception {
        String extracted = extractJson(json);
        Map<String, Object> map = objectMapper.readValue(extracted, new TypeReference<>() {});

        String question = (String) map.get("question");
        if (question == null || question.isBlank()) {
            throw new LlmException("Missing 'question' field in voice turn response");
        }

        Map<String, Boolean> topicMap = new LinkedHashMap<>();
        Object rawTopicMap = map.get("topicMap");
        if (rawTopicMap instanceof Map<?, ?> rawMap) {
            rawMap.forEach((key, value) -> {
                if (key instanceof String topicKey && value instanceof Boolean isTrue) {
                    topicMap.put(topicKey, isTrue);
                }
            });
        }

        boolean isFinalTurn = Boolean.TRUE.equals(map.get("isFinalTurn"));

        @Nullable TurnAssessment turnAssessment = null;
        Object rawAssessment = map.get("turnAssessment");
        if (rawAssessment instanceof Map<?, ?> assessmentMap) {
            String topicCovered = (String) assessmentMap.get("topicCovered");
            String strength = (String) assessmentMap.get("strength");
            String note = (String) assessmentMap.get("note");
            if (topicCovered != null && strength != null && note != null) {
                turnAssessment = new TurnAssessment(topicCovered, strength, note);
            }
        }

        return new TurnResult(question, topicMap, turnAssessment, isFinalTurn);
    }

    private static String extractJson(String raw) {
        String trimmed = raw.strip();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new LlmException("No JSON object found in LLM response");
        }
        return trimmed.substring(start, end + 1);
    }

    private void persistVoiceQuestion(InterviewSession session, int turnIndex, String question) {
        VoiceInterviewQuestion vq = new VoiceInterviewQuestion();
        vq.setSession(session);
        vq.setTurnIndex(turnIndex);
        vq.setQuestion(question);
        voiceQuestionRepository.save(vq);
    }

    private void updateTopicMap(InterviewSession session, Map<String, Boolean> topicMap) {
        if (topicMap.isEmpty()) {
            return;
        }
        try {
            session.setTopicMapJson(objectMapper.writeValueAsString(topicMap));
        } catch (Exception jsonException) {
            log.warn("Failed to serialize topicMap for session {}", session.getId(), jsonException);
        }
    }

    private static String buildOpeningPrompt(String technology, int maxTurns) {
        return "Technology: " + technology + "\n"
                + "Turn index: 0\n"
                + "Max turns: " + maxTurns + "\n"
                + "This is the opening turn — no candidate answer yet.\n"
                + "Generate an opening question and an initial topic map for " + technology + ".";
    }

    private static String buildTurnPrompt(
            String technology,
            List<VoiceInterviewQuestion> history,
            List<InterviewAnswer> answers,
            String transcript,
            int turnIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("Technology: ").append(technology).append("\n");
        sb.append("Turn index: ").append(turnIndex).append("\n\n");

        sb.append("<HISTORY>\n");
        int historyChars = 0;
        int questionNumber = 1;
        for (VoiceInterviewQuestion vq : history) {
            String questionLine = "Q" + questionNumber + ": " + vq.getQuestion() + "\n";
            String answerLine = "A" + questionNumber + ": ";
            InterviewAnswer matchingAnswer = findAnswerForTurn(answers, vq.getTurnIndex());
            if (matchingAnswer != null && matchingAnswer.getTranscript() != null) {
                answerLine += truncate(matchingAnswer.getTranscript(), MAX_TRANSCRIPT_CHARS / 2);
            } else {
                answerLine += "(no answer recorded)";
            }
            answerLine += "\n\n";
            if (historyChars + questionLine.length() + answerLine.length() > MAX_HISTORY_CHARS) {
                sb.append("(earlier turns omitted for length)\n");
                break;
            }
            sb.append(questionLine).append(answerLine);
            historyChars += questionLine.length() + answerLine.length();
            questionNumber++;
        }
        sb.append("</HISTORY>\n\n");

        sb.append("<CANDIDATE_ANSWER>\n");
        sb.append(truncate(transcript, MAX_TRANSCRIPT_CHARS));
        sb.append("\n</CANDIDATE_ANSWER>");

        return sb.toString();
    }

    @Nullable
    private static InterviewAnswer findAnswerForTurn(List<InterviewAnswer> answers, int turnIndex) {
        for (InterviewAnswer answer : answers) {
            if (answer.getVoiceQuestion() != null && answer.getVoiceQuestion().getTurnIndex() == turnIndex) {
                return answer;
            }
        }
        return null;
    }

    private static String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "[truncated]";
    }
}
