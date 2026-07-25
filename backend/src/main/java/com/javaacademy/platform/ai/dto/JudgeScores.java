package com.javaacademy.platform.ai.dto;

/** Structured scores returned by the A5 Judge from its LLM call. */
public record JudgeScores(
        int hallucinationScore, boolean answerLeakageDetected, int rubricAdherenceScore, String explanation) {}
