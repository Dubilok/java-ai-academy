package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import java.util.UUID;

public record FinishSessionResponse(
        UUID sessionId, InterviewSessionStatus status, int overallScore, EvaluationReport report) {}
