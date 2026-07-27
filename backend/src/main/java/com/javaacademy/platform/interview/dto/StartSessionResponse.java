package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewMode;
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StartSessionResponse(
        UUID sessionId,
        String technology,
        InterviewSessionStatus status,
        InterviewMode mode,
        @Nullable QuestionInSession firstQuestion,
        @Nullable String firstVoiceQuestion) {}
