package com.javaacademy.platform.interview.controller;

import com.javaacademy.platform.interview.dto.CreateInterviewQuestionRequest;
import com.javaacademy.platform.interview.dto.GenerateFlashcardsRequest;
import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.dto.UpdateInterviewQuestionRequest;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.service.FlashcardGeneratorService;
import com.javaacademy.platform.interview.service.InterviewQuestionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService questionService;
    private final FlashcardGeneratorService flashcardGeneratorService;

    @GetMapping("/interview/questions")
    public List<InterviewQuestionResponse> listQuestions(
            @RequestParam(required = false) @Nullable String technology,
            @RequestParam(required = false) @Nullable String category,
            @RequestParam(required = false) @Nullable InterviewDifficulty difficulty) {
        return questionService.findQuestions(technology, category, difficulty);
    }

    @PostMapping("/admin/interview/questions/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InterviewQuestionResponse> generateQuestions(@Valid @RequestBody GenerateFlashcardsRequest request) {
        return flashcardGeneratorService.generate(request);
    }

    @PostMapping("/admin/interview/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InterviewQuestionResponse createQuestion(@Valid @RequestBody CreateInterviewQuestionRequest request) {
        return questionService.create(request);
    }

    @PutMapping("/admin/interview/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public InterviewQuestionResponse updateQuestion(
            @PathVariable UUID id, @Valid @RequestBody UpdateInterviewQuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/admin/interview/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteQuestion(@PathVariable UUID id) {
        questionService.delete(id);
    }
}
