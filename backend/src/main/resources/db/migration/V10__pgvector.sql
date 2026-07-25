-- Enable pgvector extension (requires pg16 + pgvector image or extension installed on the server)
CREATE EXTENSION IF NOT EXISTS vector;

-- Stores chunked lecture content with embeddings for RAG retrieval
CREATE TABLE lecture_chunks (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lecture_id    UUID NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    chunk_index   INT  NOT NULL,
    chunk_text    TEXT NOT NULL,
    embedding     vector(1024),          -- Titan Embeddings V2 produces 1024-dim vectors
    indexed_at    TIMESTAMPTZ DEFAULT now() NOT NULL,

    UNIQUE (lecture_id, chunk_index)
);

-- IVFFlat index for approximate nearest-neighbour search (cosine distance)
-- Built after the first batch of embeddings is loaded; index is empty until rows exist.
CREATE INDEX idx_lecture_chunks_embedding ON lecture_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);
CREATE INDEX idx_lecture_chunks_lecture_id ON lecture_chunks(lecture_id);
