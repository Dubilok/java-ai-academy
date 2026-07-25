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
            """
            SELECT COUNT(log), COALESCE(SUM(log.promptTokens), 0),
                   COALESCE(SUM(log.completionTokens), 0), SUM(log.costUsd)
            FROM AiGenerationLog log
            WHERE (:from IS NULL OR log.createdAt >= :from)
              AND (:to   IS NULL OR log.createdAt <= :to)
            """)
    List<Object[]> findTotals(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-agent breakdown: agent, count, promptTokens, completionTokens, cost, avgLatency. */
    @Query(
            """
            SELECT log.agent, COUNT(log), COALESCE(SUM(log.promptTokens), 0),
                   COALESCE(SUM(log.completionTokens), 0), SUM(log.costUsd), AVG(log.latencyMs)
            FROM AiGenerationLog log
            WHERE (:from IS NULL OR log.createdAt >= :from)
              AND (:to   IS NULL OR log.createdAt <= :to)
            GROUP BY log.agent
            ORDER BY SUM(log.costUsd) DESC NULLS LAST
            """)
    List<Object[]> findByAgent(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-user breakdown (only rows with a non-null userId): userId, email, count, cost. */
    @Query(
            """
            SELECT log.userId, u.email, COUNT(log), SUM(log.costUsd)
            FROM AiGenerationLog log
            JOIN User u ON u.id = log.userId
            WHERE log.userId IS NOT NULL
              AND (:from IS NULL OR log.createdAt >= :from)
              AND (:to   IS NULL OR log.createdAt <= :to)
            GROUP BY log.userId, u.email
            ORDER BY SUM(log.costUsd) DESC NULLS LAST
            """)
    List<Object[]> findByUser(@Param("from") Instant from, @Param("to") Instant to);

    /** Per-course breakdown (only rows with a non-null courseId): courseId, title, count, cost. */
    @Query(
            """
            SELECT log.courseId, c.title, COUNT(log), SUM(log.costUsd)
            FROM AiGenerationLog log
            JOIN Course c ON c.id = log.courseId
            WHERE log.courseId IS NOT NULL
              AND (:from IS NULL OR log.createdAt >= :from)
              AND (:to   IS NULL OR log.createdAt <= :to)
            GROUP BY log.courseId, c.title
            ORDER BY SUM(log.costUsd) DESC NULLS LAST
            """)
    List<Object[]> findByCourse(@Param("from") Instant from, @Param("to") Instant to);
}
