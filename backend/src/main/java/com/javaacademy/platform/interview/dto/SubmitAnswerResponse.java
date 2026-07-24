package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SubmitAnswerResponse(
        UUID sessionId,
        UUID answerId,
        InterviewSessionStatus sessionStatus,
        @Nullable QuestionInSession nextQuestion) {}
