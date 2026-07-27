package com.javaacademy.platform.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.AnthropicProperties;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.interview.dto.TurnResult;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import com.javaacademy.platform.interview.entity.InterviewSession;
import com.javaacademy.platform.interview.entity.VoiceInterviewQuestion;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.VoiceInterviewQuestionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VoiceInterviewOrchestratorTest {

    static final String TECHNOLOGY = "Java";
    static final int MAX_TURNS = 5;

    static final String OPENING_RESPONSE =
            """
            {
              "question": "Can you explain the purpose of the JVM?",
              "topicMap": { "jvm": false, "collections": false, "concurrency": false },
              "isFinalTurn": false
            }
            """;

    static final String FOLLOWUP_RESPONSE =
            """
            {
              "question": "How does the garbage collector decide what to collect?",
              "topicMap": { "jvm": true, "collections": false, "concurrency": false },
              "turnAssessment": {
                "topicCovered": "jvm",
                "strength": "STRONG",
                "note": "The candidate explained the role of the JVM clearly."
              },
              "isFinalTurn": false
            }
            """;

    static final String FINAL_TURN_RESPONSE =
            """
            {
              "question": "Looking back, which Java concept do you feel most confident about?",
              "topicMap": { "jvm": true, "collections": true, "concurrency": true },
              "turnAssessment": {
                "topicCovered": "concurrency",
                "strength": "ADEQUATE",
                "note": "Answer covered the basics but missed virtual threads."
              },
              "isFinalTurn": true
            }
            """;

    LlmClient llmClient;
    AnthropicProperties anthropicProperties;
    VoiceInterviewQuestionRepository voiceQuestionRepository;
    InterviewAnswerRepository answerRepository;
    VoiceInterviewOrchestrator orchestrator;

    InterviewSession session;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        anthropicProperties = new AnthropicProperties(
                "key",
                "https://api.anthropic.com",
                "v1",
                "claude-haiku-4-5",
                "claude-haiku-4-5",
                "claude-sonnet-4-6",
                3,
                200L,
                1.0,
                5.0);
        voiceQuestionRepository = mock(VoiceInterviewQuestionRepository.class);
        answerRepository = mock(InterviewAnswerRepository.class);

        when(voiceQuestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator = new VoiceInterviewOrchestrator(
                llmClient,
                anthropicProperties,
                voiceQuestionRepository,
                answerRepository,
                new ObjectMapper(),
                "SYSTEM PROMPT");

        session = mock(InterviewSession.class);
        when(session.getId()).thenReturn(UUID.randomUUID());
        when(session.getTechnology()).thenReturn(TECHNOLOGY);
    }

    @Test
    void generateOpeningQuestion_returnsQuestionAndTopicMap() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(OPENING_RESPONSE, 50, 150));

        TurnResult result = orchestrator.generateOpeningQuestion(session, MAX_TURNS);

        assertThat(result.question()).isEqualTo("Can you explain the purpose of the JVM?");
        assertThat(result.topicMap()).containsKeys("jvm", "collections", "concurrency");
        assertThat(result.topicMap().get("jvm")).isFalse();
        assertThat(result.isFinalTurn()).isFalse();
        assertThat(result.turnAssessment()).isNull();
    }

    @Test
    void generateOpeningQuestion_persistsVoiceQuestion() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(OPENING_RESPONSE, 50, 150));

        orchestrator.generateOpeningQuestion(session, MAX_TURNS);

        verify(voiceQuestionRepository).save(any(VoiceInterviewQuestion.class));
    }

    @Test
    void generateNextQuestion_updatesTopicMapAndIncludesAssessment() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(FOLLOWUP_RESPONSE, 60, 200));
        when(voiceQuestionRepository.findBySessionIdOrderByTurnIndex(any())).thenReturn(List.of());
        when(answerRepository.findBySessionIdWithVoiceQuestionOrderByCreatedAt(any()))
                .thenReturn(List.of());

        TurnResult result = orchestrator.generateNextQuestion(session, "The JVM interprets bytecode.", 1);

        assertThat(result.question()).contains("garbage collector");
        assertThat(result.topicMap().get("jvm")).isTrue();
        assertThat(result.topicMap().get("collections")).isFalse();
        assertThat(result.turnAssessment()).isNotNull();
        assertThat(result.turnAssessment().topicCovered()).isEqualTo("jvm");
        assertThat(result.turnAssessment().strength()).isEqualTo("STRONG");
        assertThat(result.isFinalTurn()).isFalse();
    }

    @Test
    void generateNextQuestion_marksIsFinalTurnWhenAllTopicsCovered() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(FINAL_TURN_RESPONSE, 70, 220));
        when(voiceQuestionRepository.findBySessionIdOrderByTurnIndex(any())).thenReturn(List.of());
        when(answerRepository.findBySessionIdWithVoiceQuestionOrderByCreatedAt(any()))
                .thenReturn(List.of());

        TurnResult result = orchestrator.generateNextQuestion(session, "I know concurrency well.", 4);

        assertThat(result.isFinalTurn()).isTrue();
        assertThat(result.topicMap().get("concurrency")).isTrue();
    }

    @Test
    void generateNextQuestion_includesHistoryInPrompt() {
        VoiceInterviewQuestion previousQuestion = mock(VoiceInterviewQuestion.class);
        when(previousQuestion.getTurnIndex()).thenReturn(0);
        when(previousQuestion.getQuestion()).thenReturn("What is the JVM?");

        InterviewAnswer previousAnswer = mock(InterviewAnswer.class);
        when(previousAnswer.getTranscript()).thenReturn("It runs Java bytecode.");
        VoiceInterviewQuestion vq = mock(VoiceInterviewQuestion.class);
        when(vq.getTurnIndex()).thenReturn(0);
        when(previousAnswer.getVoiceQuestion()).thenReturn(vq);

        when(voiceQuestionRepository.findBySessionIdOrderByTurnIndex(any())).thenReturn(List.of(previousQuestion));
        when(answerRepository.findBySessionIdWithVoiceQuestionOrderByCreatedAt(any()))
                .thenReturn(List.of(previousAnswer));
        when(llmClient.complete(any())).thenReturn(new LlmResponse(FOLLOWUP_RESPONSE, 60, 200));

        orchestrator.generateNextQuestion(session, "It runs bytecode.", 1);

        verify(llmClient).complete(any());
    }

    @Test
    void generateOpeningQuestion_invalidJson_throwsLlmException() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse("not json at all", 10, 20));

        assertThatThrownBy(() -> orchestrator.generateOpeningQuestion(session, MAX_TURNS))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void generateOpeningQuestion_missingQuestionField_throwsLlmException() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse("{\"isFinalTurn\": false}", 10, 20));

        assertThatThrownBy(() -> orchestrator.generateOpeningQuestion(session, MAX_TURNS))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Missing 'question'");
    }

    @Test
    void generateOpeningQuestion_usesHaikuModel() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(OPENING_RESPONSE, 50, 150));

        orchestrator.generateOpeningQuestion(session, MAX_TURNS);

        verify(llmClient).complete(argThat(req -> "claude-haiku-4-5".equals(req.model())));
    }

    private static <T> T argThat(java.util.function.Predicate<T> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
