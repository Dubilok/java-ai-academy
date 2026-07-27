package com.javaacademy.platform.interview.dto;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public record TurnResult(
        String question, Map<String, Boolean> topicMap, @Nullable TurnAssessment turnAssessment, boolean isFinalTurn) {}
