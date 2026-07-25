package com.javaacademy.platform.ai.rag;

import com.pgvector.PGvector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * pgvector implementation of {@link VectorStore}.
 *
 * <p>Uses the {@code lecture_chunks} table created by {@code V10__pgvector.sql}. JDBC is used
 * directly because Spring Data JPA has no native vector type support. The {@link PGvector} class
 * from {@code com.pgvector:pgvector} handles the PostgreSQL wire-protocol encoding.
 */
@Slf4j
@Repository
public class PgVectorStore implements VectorStore {

    private static final String UPSERT_SQL =
            """
            INSERT INTO lecture_chunks (lecture_id, chunk_index, chunk_text, embedding)
            VALUES (?::uuid, ?, ?, ?::vector)
            ON CONFLICT (lecture_id, chunk_index)
            DO UPDATE SET chunk_text = EXCLUDED.chunk_text,
                          embedding  = EXCLUDED.embedding,
                          indexed_at = now()
            """;

    private static final String DELETE_SQL = "DELETE FROM lecture_chunks WHERE lecture_id = ?::uuid";

    private static final String SEARCH_SQL =
            """
            SELECT lecture_id, chunk_index, chunk_text,
                   1 - (embedding <=> ?::vector) AS score
            FROM lecture_chunks
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;

    private final DataSource dataSource;

    public PgVectorStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void upsert(UUID lectureId, int chunkIndex, String chunkText, float[] embedding) {
        String vectorLiteral = toVectorLiteral(embedding);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, lectureId.toString());
            statement.setInt(2, chunkIndex);
            statement.setString(3, chunkText);
            statement.setString(4, vectorLiteral);
            statement.executeUpdate();
        } catch (SQLException sqlException) {
            throw new VectorStoreException("Failed to upsert lecture chunk: " + sqlException.getMessage());
        }
    }

    @Override
    public void deleteByLecture(UUID lectureId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, lectureId.toString());
            int rowsDeleted = statement.executeUpdate();
            log.debug("Deleted {} chunks for lecture {}", rowsDeleted, lectureId);
        } catch (SQLException sqlException) {
            throw new VectorStoreException("Failed to delete lecture chunks: " + sqlException.getMessage());
        }
    }

    @Override
    public List<VectorMatch> search(float[] queryEmbedding, int topK) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        List<VectorMatch> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(SEARCH_SQL)) {
            statement.setString(1, vectorLiteral);
            statement.setString(2, vectorLiteral);
            statement.setInt(3, topK);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID lectureId = UUID.fromString(resultSet.getString("lecture_id"));
                    int chunkIndex = resultSet.getInt("chunk_index");
                    String chunkText = resultSet.getString("chunk_text");
                    double score = resultSet.getDouble("score");
                    results.add(new VectorMatch(lectureId, chunkIndex, chunkText, score));
                }
            }
        } catch (SQLException sqlException) {
            throw new VectorStoreException("Failed to search lecture chunks: " + sqlException.getMessage());
        }
        return results;
    }

    private static String toVectorLiteral(float[] embedding) {
        return new PGvector(embedding).getValue();
    }
}
