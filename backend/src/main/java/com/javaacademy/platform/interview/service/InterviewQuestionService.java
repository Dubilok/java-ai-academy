package com.javaacademy.platform.interview.service;

import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.CreateInterviewQuestionRequest;
import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.dto.UpdateInterviewQuestionRequest;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.mapper.InterviewQuestionMapper;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private final InterviewQuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> findQuestions(
            @Nullable String technology, @Nullable String category, @Nullable InterviewDifficulty difficulty) {
        return questionRepository.findByFilters(technology, category, difficulty).stream()
                .map(InterviewQuestionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewQuestionResponse findById(UUID id) {
        return questionRepository
                .findById(id)
                .map(InterviewQuestionMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview question not found: " + id));
    }

    @Transactional
    public InterviewQuestionResponse create(CreateInterviewQuestionRequest request) {
        InterviewQuestion question = new InterviewQuestion();
        question.setTechnology(request.technology());
        question.setCategory(request.category());
        question.setQuestion(request.question());
        question.setShortAnswer(request.shortAnswer());
        question.setDetailedExplanation(request.detailedExplanation());
        question.setDifficulty(request.difficulty());
        return InterviewQuestionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public InterviewQuestionResponse update(UUID id, UpdateInterviewQuestionRequest request) {
        InterviewQuestion question = questionRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview question not found: " + id));
        question.setTechnology(request.technology());
        question.setCategory(request.category());
        question.setQuestion(request.question());
        question.setShortAnswer(request.shortAnswer());
        question.setDetailedExplanation(request.detailedExplanation());
        question.setDifficulty(request.difficulty());
        return InterviewQuestionMapper.toResponse(question);
    }

    @Transactional
    public void delete(UUID id) {
        if (!questionRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Interview question not found: " + id);
        }
        questionRepository.deleteById(id);
    }
}
