package com.javaacademy.platform.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for {@link PgVectorStore} against a real pgvector-enabled PostgreSQL instance.
 *
 * <p>Uses {@code pgvector/pgvector:pg16} Testcontainers image which has the vector extension
 * pre-installed. Flyway cleans and re-migrates before each test to guarantee an empty
 * {@code lecture_chunks} table.
 */
@Testcontainers
class PgVectorStoreTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    DataSource dataSource;
    PgVectorStore store;

    @BeforeEach
    void setUp() {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(postgres.getJdbcUrl());
        pgDataSource.setUser(postgres.getUsername());
        pgDataSource.setPassword(postgres.getPassword());
        dataSource = pgDataSource;

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        store = new PgVectorStore(dataSource);
    }

    @Test
    void upsert_andSearch_returnsMostSimilarChunk() {
        UUID lectureId = insertLecture();
        float[] embedding = unitVector(1024, 0);

        store.upsert(lectureId, 0, "Java records are immutable data holders.", embedding);

        List<VectorMatch> results = store.search(embedding, 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).lectureId()).isEqualTo(lectureId);
        assertThat(results.get(0).chunkText()).isEqualTo("Java records are immutable data holders.");
        assertThat(results.get(0).score()).isGreaterThan(0.99);
    }

    @Test
    void upsert_sameKey_updatesExistingRow() {
        UUID lectureId = insertLecture();
        float[] embedding = unitVector(1024, 0);

        store.upsert(lectureId, 0, "Original text", embedding);
        store.upsert(lectureId, 0, "Updated text", embedding);

        List<VectorMatch> results = store.search(embedding, 5);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkText()).isEqualTo("Updated text");
    }

    @Test
    void deleteByLecture_removesAllChunksForLecture() {
        UUID lectureA = insertLecture();
        UUID lectureB = insertLecture();
        float[] embeddingA = unitVector(1024, 0);
        float[] embeddingB = unitVector(1024, 1);

        store.upsert(lectureA, 0, "Content A", embeddingA);
        store.upsert(lectureB, 0, "Content B", embeddingB);

        store.deleteByLecture(lectureA);

        List<VectorMatch> results = store.search(embeddingA, 10);
        assertThat(results).noneMatch(match -> match.lectureId().equals(lectureA));
        assertThat(results).anyMatch(match -> match.lectureId().equals(lectureB));
    }

    @Test
    void search_emptyStore_returnsEmptyList() {
        float[] queryEmbedding = unitVector(1024, 0);

        List<VectorMatch> results = store.search(queryEmbedding, 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_topKLimitsResults() {
        UUID lectureId = insertLecture();
        float[] baseEmbedding = unitVector(1024, 0);

        for (int chunkIndex = 0; chunkIndex < 10; chunkIndex++) {
            store.upsert(lectureId, chunkIndex, "Chunk " + chunkIndex, baseEmbedding);
        }

        List<VectorMatch> results = store.search(baseEmbedding, 3);

        assertThat(results).hasSize(3);
    }

    /**
     * Inserts a minimal course → module → lecture chain and returns the lecture UUID.
     * Each call creates independent rows so multiple calls in one test don't conflict.
     */
    private UUID insertLecture() {
        try (Connection conn = dataSource.getConnection()) {
            UUID courseId;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO courses (title, technology) VALUES ('Test Course', 'Java') RETURNING id")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    courseId = UUID.fromString(rs.getString(1));
                }
            }

            UUID moduleId;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO modules (course_id, title, order_index) VALUES (?::uuid, 'Test Module', 1) RETURNING id")) {
                stmt.setString(1, courseId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    moduleId = UUID.fromString(rs.getString(1));
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lectures (module_id, title, order_index) VALUES (?::uuid, 'Test Lecture', 1) RETURNING id")) {
                stmt.setString(1, moduleId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return UUID.fromString(rs.getString(1));
                }
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(
                    "Failed to insert test lecture fixture: " + sqlException.getMessage(), sqlException);
        }
    }

    /** Returns a unit vector of the given dimension with all values equal, normalised to 1. */
    private static float[] unitVector(int dimensions, int salt) {
        float[] vector = new float[dimensions];
        float value = (float) (1.0 / Math.sqrt(dimensions) + salt * 0.0001);
        for (int index = 0; index < dimensions; index++) {
            vector[index] = value;
        }
        return vector;
    }
}
