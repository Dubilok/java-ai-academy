package com.javaacademy.platform.ai.rag;

import java.util.UUID;

/**
 * A single result returned by {@link VectorStore#search}.
 *
 * @param lectureId  the lecture the chunk belongs to
 * @param chunkIndex zero-based position of this chunk within the lecture
 * @param chunkText  the raw text content of the chunk
 * @param score      cosine similarity score (higher = more similar)
 */
public record VectorMatch(UUID lectureId, int chunkIndex, String chunkText, double score) {}
