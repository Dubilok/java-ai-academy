package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import java.util.UUID;

public record StartSessionResponse(
        UUID sessionId, String technology, InterviewSessionStatus status, QuestionInSession firstQuestion) {}
