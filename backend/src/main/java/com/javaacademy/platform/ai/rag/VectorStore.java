package com.javaacademy.platform.ai.rag;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over a vector index for semantic search.
 *
 * <p>Local development uses {@link PgVectorStore}. Cloud deployments may use Azure AI Search
 * behind the same interface (see §13 Q1 in CLAUDE.md).
 */
public interface VectorStore {

    /**
     * Inserts or replaces a chunk for the given lecture + chunkIndex.
     *
     * @param lectureId  the lecture this chunk belongs to
     * @param chunkIndex zero-based position within the lecture (used for UPSERT key)
     * @param chunkText  the raw text to store alongside the vector
     * @param embedding  the dense vector (must match the store's configured dimension)
     */
    void upsert(UUID lectureId, int chunkIndex, String chunkText, float[] embedding);

    /**
     * Deletes all chunks for the given lecture (called when a lecture is updated/deleted).
     *
     * @param lectureId the lecture whose chunks should be removed
     */
    void deleteByLecture(UUID lectureId);

    /**
     * Returns the top-K most similar chunks by cosine similarity.
     *
     * @param queryEmbedding the query vector (same dimension as indexed embeddings)
     * @param topK           maximum number of results to return
     * @return matches ordered by descending similarity score
     */
    List<VectorMatch> search(float[] queryEmbedding, int topK);
}
