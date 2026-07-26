package com.javaacademy.platform.ai.repository;

import com.javaacademy.platform.ai.entity.AiGenerationLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    /** Total requests, tokens, and cost in the time window (inclusive). */
    @Query(
            value =
                    """
                    SELECT COUNT(id), COALESCE(SUM(prompt_tokens), 0),
                           COALESCE(SUM(completion_tokens), 0), SUM(cost_usd)
                    FROM ai_generation_log
                    WHERE (CAST(:from AS timestamptz) IS NULL OR created_at >= :from)
                      AND (CAST(:to   AS timestamptz) IS NULL OR created_at <= :to)
                    """,
            nativeQuery = true)
    List<Object[]> findTotals(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-agent breakdown: agent, count, promptTokens, completionTokens, cost, avgLatency. */
    @Query(
            value =
                    """
                    SELECT agent, COUNT(id), COALESCE(SUM(prompt_tokens), 0),
                           COALESCE(SUM(completion_tokens), 0), SUM(cost_usd), AVG(latency_ms)
                    FROM ai_generation_log
                    WHERE (CAST(:from AS timestamptz) IS NULL OR created_at >= :from)
                      AND (CAST(:to   AS timestamptz) IS NULL OR created_at <= :to)
                    GROUP BY agent
                    ORDER BY SUM(cost_usd) DESC NULLS LAST
                    """,
            nativeQuery = true)
    List<Object[]> findByAgent(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-user breakdown (only rows with a non-null userId): userId, email, count, cost. */
    @Query(
            value =
                    """
                    SELECT l.user_id, u.email, COUNT(l.id), SUM(l.cost_usd)
                    FROM ai_generation_log l
                    JOIN users u ON u.id = l.user_id
                    WHERE l.user_id IS NOT NULL
                      AND (CAST(:from AS timestamptz) IS NULL OR l.created_at >= :from)
                      AND (CAST(:to   AS timestamptz) IS NULL OR l.created_at <= :to)
                    GROUP BY l.user_id, u.email
                    ORDER BY SUM(l.cost_usd) DESC NULLS LAST
                    """,
            nativeQuery = true)
    List<Object[]> findByUser(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-course breakdown (only rows with a non-null courseId): courseId, title, count, cost. */
    @Query(
            value =
                    """
                    SELECT l.course_id, c.title, COUNT(l.id), SUM(l.cost_usd)
                    FROM ai_generation_log l
                    JOIN courses c ON c.id = l.course_id
                    WHERE l.course_id IS NOT NULL
                      AND (CAST(:from AS timestamptz) IS NULL OR l.created_at >= :from)
                      AND (CAST(:to   AS timestamptz) IS NULL OR l.created_at <= :to)
                    GROUP BY l.course_id, c.title
                    ORDER BY SUM(l.cost_usd) DESC NULLS LAST
                    """,
            nativeQuery = true)
    List<Object[]> findByCourse(@Param("from") Instant from, @Param("to") Instant to);
}
