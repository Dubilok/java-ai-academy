package com.javaacademy.platform.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LectureIndexingServiceTest {

    static final float[] FAKE_EMBEDDING = new float[1024];
    static final UUID LECTURE_ID = UUID.randomUUID();

    EmbeddingClient mockEmbeddingClient;
    VectorStore mockVectorStore;
    LectureIndexingService service;

    @BeforeEach
    void setUp() {
        mockEmbeddingClient = mock(EmbeddingClient.class);
        mockVectorStore = mock(VectorStore.class);
        when(mockEmbeddingClient.embed(anyString())).thenReturn(FAKE_EMBEDDING);
        service = new LectureIndexingService(mockEmbeddingClient, mockVectorStore);
    }

    // ── indexLecture ──────────────────────────────────────────────────────────

    @Test
    void indexLecture_shortContent_producesOneChunk() {
        String shortContent = "Java records are immutable data classes.";

        service.indexLecture(LECTURE_ID, shortContent);

        verify(mockVectorStore).deleteByLecture(LECTURE_ID);
        verify(mockEmbeddingClient).embed(shortContent);
        verify(mockVectorStore).upsert(LECTURE_ID, 0, shortContent, FAKE_EMBEDDING);
    }

    @Test
    void indexLecture_longContent_producesMultipleChunks() {
        String content = "A".repeat(2000);

        service.indexLecture(LECTURE_ID, content);

        verify(mockVectorStore).deleteByLecture(LECTURE_ID);
        // 800-char chunks, 100-char overlap → step=700; chunks at 0,700,1400 → 3 chunks
        verify(mockEmbeddingClient, times(3)).embed(anyString());
        verify(mockVectorStore, times(3)).upsert(eq(LECTURE_ID), anyInt(), anyString(), any(float[].class));
    }

    @Test
    void indexLecture_deletesExistingChunksBeforeInserting() {
        service.indexLecture(LECTURE_ID, "content");

        InOrder orderedVerify = inOrder(mockVectorStore, mockEmbeddingClient);
        orderedVerify.verify(mockVectorStore).deleteByLecture(LECTURE_ID);
        orderedVerify.verify(mockEmbeddingClient).embed(anyString());
        orderedVerify.verify(mockVectorStore).upsert(eq(LECTURE_ID), eq(0), anyString(), any(float[].class));
    }

    @Test
    void removeLecture_delegatesToVectorStore() {
        service.removeLecture(LECTURE_ID);

        verify(mockVectorStore).deleteByLecture(LECTURE_ID);
    }

    @Test
    void search_embedsQueryAndSearchesVectorStore() {
        String query = "What are Java records?";
        List<VectorMatch> expectedMatches =
                List.of(new VectorMatch(LECTURE_ID, 0, "Java records are immutable data classes.", 0.95));
        when(mockVectorStore.search(FAKE_EMBEDDING, 3)).thenReturn(expectedMatches);

        List<VectorMatch> results = service.search(query, 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).score()).isEqualTo(0.95);
        verify(mockEmbeddingClient).embed(query);
        verify(mockVectorStore).search(FAKE_EMBEDDING, 3);
    }

    // ── chunk() static helper ─────────────────────────────────────────────────

    @Test
    void chunk_emptyString_returnsEmptyList() {
        assertThat(LectureIndexingService.chunk("", 800, 100)).isEmpty();
    }

    @Test
    void chunk_nullString_returnsEmptyList() {
        assertThat(LectureIndexingService.chunk(null, 800, 100)).isEmpty();
    }

    @Test
    void chunk_exactlyOneChunk_returnsSingleElement() {
        String text = "A".repeat(800);
        List<String> chunks = LectureIndexingService.chunk(text, 800, 100);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void chunk_overlapPreservesContext() {
        String text = "A".repeat(900);
        List<String> chunks = LectureIndexingService.chunk(text, 800, 100);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(800);
        assertThat(chunks.get(1)).hasSize(200);
        // Last 100 chars of chunk 0 equal first 100 chars of chunk 1
        assertThat(chunks.get(0).substring(700)).isEqualTo(chunks.get(1).substring(0, 100));
    }

    @Test
    void chunk_overlapGreaterThanChunkSize_throwsIllegalArgument() {
        assertThatThrownBy(() -> LectureIndexingService.chunk("text", 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
