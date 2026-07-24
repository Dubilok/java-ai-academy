package com.javaacademy.platform.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.CreateInterviewQuestionRequest;
import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.dto.UpdateInterviewQuestionRequest;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InterviewQuestionServiceTest {

    static final UUID QUESTION_ID = UUID.randomUUID();

    InterviewQuestionRepository questionRepository;
    InterviewQuestionService service;

    @BeforeEach
    void setUp() {
        questionRepository = mock(InterviewQuestionRepository.class);
        service = new InterviewQuestionService(questionRepository);
    }

    // ── findQuestions ──────────────────────────────────────────────────────────

    @Test
    void findQuestions_noFilters_returnsAll() {
        when(questionRepository.findByFilters(null, null, null)).thenReturn(List.of(question()));

        List<InterviewQuestionResponse> result = service.findQuestions(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().technology()).isEqualTo("Java");
    }

    @Test
    void findQuestions_withTechnologyFilter_delegatesToRepository() {
        when(questionRepository.findByFilters("Java", null, null)).thenReturn(List.of(question()));

        List<InterviewQuestionResponse> result = service.findQuestions("Java", null, null);

        assertThat(result).hasSize(1);
        verify(questionRepository).findByFilters("Java", null, null);
    }

    @Test
    void findQuestions_withAllFilters_delegatesToRepository() {
        when(questionRepository.findByFilters("Spring", "Beans", InterviewDifficulty.ADVANCED))
                .thenReturn(List.of());

        List<InterviewQuestionResponse> result = service.findQuestions("Spring", "Beans", InterviewDifficulty.ADVANCED);

        assertThat(result).isEmpty();
        verify(questionRepository).findByFilters("Spring", "Beans", InterviewDifficulty.ADVANCED);
    }

    // ── findById ───────────────────────────────────────────────────────────────

    @Test
    void findById_exists_returnsResponse() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question()));

        InterviewQuestionResponse result = service.findById(QUESTION_ID);

        assertThat(result.id()).isEqualTo(QUESTION_ID);
        assertThat(result.difficulty()).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void findById_notFound_throws404() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(QUESTION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        CreateInterviewQuestionRequest request = new CreateInterviewQuestionRequest(
                "Java",
                "OOP",
                "What is polymorphism?",
                "Short answer",
                "Long explanation",
                InterviewDifficulty.BEGINNER);
        when(questionRepository.save(any())).thenAnswer(invocation -> {
            InterviewQuestion saved = invocation.getArgument(0);
            saved.setTechnology("Java"); // simulate saved entity
            return savedQuestion(saved);
        });

        InterviewQuestionResponse result = service.create(request);

        assertThat(result.technology()).isEqualTo("Java");
        assertThat(result.difficulty()).isEqualTo(InterviewDifficulty.BEGINNER);
        verify(questionRepository).save(any());
    }

    @Test
    void create_withNullShortAnswer_savesSuccessfully() {
        CreateInterviewQuestionRequest request = new CreateInterviewQuestionRequest(
                "Java", "OOP", "What is encapsulation?", null, null, InterviewDifficulty.INTERMEDIATE);
        when(questionRepository.save(any())).thenAnswer(invocation -> savedQuestion(invocation.getArgument(0)));

        InterviewQuestionResponse result = service.create(request);

        assertThat(result.shortAnswer()).isNull();
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_existingQuestion_updatesAllFields() {
        InterviewQuestion existing = question();
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(existing));
        UpdateInterviewQuestionRequest request = new UpdateInterviewQuestionRequest(
                "Spring",
                "IoC",
                "What is dependency injection?",
                "Updated short",
                "Updated detail",
                InterviewDifficulty.ADVANCED);

        InterviewQuestionResponse result = service.update(QUESTION_ID, request);

        assertThat(result.technology()).isEqualTo("Spring");
        assertThat(result.category()).isEqualTo("IoC");
        assertThat(result.difficulty()).isEqualTo(InterviewDifficulty.ADVANCED);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void update_notFound_throws404() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());
        UpdateInterviewQuestionRequest request = new UpdateInterviewQuestionRequest(
                "Java", "OOP", "Question?", null, null, InterviewDifficulty.BEGINNER);

        assertThatThrownBy(() -> service.update(QUESTION_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_existingQuestion_deletesById() {
        when(questionRepository.existsById(QUESTION_ID)).thenReturn(true);

        service.delete(QUESTION_ID);

        verify(questionRepository).deleteById(QUESTION_ID);
    }

    @Test
    void delete_notFound_throws404() {
        when(questionRepository.existsById(QUESTION_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(QUESTION_ID))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private InterviewQuestion question() {
        InterviewQuestion interviewQuestion = new InterviewQuestion();
        interviewQuestion.setTechnology("Java");
        interviewQuestion.setCategory("Core");
        interviewQuestion.setQuestion("What is the JVM?");
        interviewQuestion.setShortAnswer("Java Virtual Machine");
        interviewQuestion.setDetailedExplanation("The JVM is the runtime environment for Java bytecode.");
        interviewQuestion.setDifficulty(InterviewDifficulty.INTERMEDIATE);
        return savedQuestion(interviewQuestion);
    }

    private InterviewQuestion savedQuestion(InterviewQuestion interviewQuestion) {
        try {
            var idField = InterviewQuestion.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(interviewQuestion, QUESTION_ID);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
        return interviewQuestion;
    }
}
