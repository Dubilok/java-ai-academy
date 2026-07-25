package com.javaacademy.platform.interview.dto;

import java.util.UUID;

/** A single lecture chunk retrieved from the RAG index to ground an interview evaluation. */
public record RagCitation(UUID lectureId, int chunkIndex, String snippet) {}
