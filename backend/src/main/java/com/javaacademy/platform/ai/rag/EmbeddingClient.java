package com.javaacademy.platform.ai.rag;

/**
 * Produces dense vector embeddings from text.
 *
 * <p>Implementations are provider-specific (Bedrock Titan, OpenAI, etc.). The vector dimension
 * must be consistent across all implementations used with the same {@link VectorStore} index.
 */
public interface EmbeddingClient {

    /** Returns the number of dimensions in every vector produced by this client. */
    int dimensions();

    /** Embeds the given text and returns a dense float array of length {@link #dimensions()}. */
    float[] embed(String text);
}
