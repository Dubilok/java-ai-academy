package com.javaacademy.platform.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Chunks lecture markdown into overlapping windows and indexes them via {@link VectorStore}.
 *
 * <p>Chunk strategy: fixed-size by character count (default 800 chars) with 100-char overlap to
 * preserve context across chunk boundaries. This is intentionally simple and can be upgraded to a
 * sentence-aware splitter later without changing the interface.
 */
@Slf4j
@Service
@ConditionalOnBean(EmbeddingClient.class)
public class LectureIndexingService {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public LectureIndexingService(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Indexes all content for the given lecture.
     *
     * <p>Deletes existing chunks first so re-indexing (on content update) is idempotent.
     *
     * @param lectureId      the lecture to index
     * @param contentMarkdown the full markdown body
     */
    public void indexLecture(UUID lectureId, String contentMarkdown) {
        vectorStore.deleteByLecture(lectureId);
        List<String> chunks = chunk(contentMarkdown, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
        log.debug("Indexing lecture {} — {} chunk(s)", lectureId, chunks.size());
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            String chunkText = chunks.get(chunkIndex);
            float[] embedding = embeddingClient.embed(chunkText);
            vectorStore.upsert(lectureId, chunkIndex, chunkText, embedding);
        }
        log.info("Indexed lecture {} with {} chunk(s)", lectureId, chunks.size());
    }

    /**
     * Removes all indexed chunks for the given lecture.
     *
     * @param lectureId the lecture whose index entries should be removed
     */
    public void removeLecture(UUID lectureId) {
        vectorStore.deleteByLecture(lectureId);
    }

    /**
     * Searches for the top-K most semantically similar chunks across all indexed lectures.
     *
     * @param query the user's natural-language query
     * @param topK  the maximum number of results to return
     * @return matches ordered by descending similarity
     */
    public List<VectorMatch> search(String query, int topK) {
        float[] queryEmbedding = embeddingClient.embed(query);
        return vectorStore.search(queryEmbedding, topK);
    }

    /**
     * Splits {@code text} into overlapping chunks of at most {@code chunkSize} characters,
     * stepping forward by {@code chunkSize - overlap} characters each iteration.
     */
    static List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        int step = chunkSize - overlap;
        if (step <= 0) {
            throw new IllegalArgumentException("overlap must be less than chunkSize");
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start += step;
        }
        return chunks;
    }
}
