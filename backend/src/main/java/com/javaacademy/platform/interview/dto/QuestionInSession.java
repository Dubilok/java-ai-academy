package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import java.util.UUID;

public record QuestionInSession(
        UUID questionId, String questionText, String category, InterviewDifficulty difficulty) {}
