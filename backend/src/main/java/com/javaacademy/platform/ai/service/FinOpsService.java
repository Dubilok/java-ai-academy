package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.dto.AgentUsageSummary;
import com.javaacademy.platform.ai.dto.AiUsageResponse;
import com.javaacademy.platform.ai.dto.CourseUsageSummary;
import com.javaacademy.platform.ai.dto.UserUsageSummary;
import com.javaacademy.platform.ai.enums.AgentType;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregates token usage and cost from {@code ai_generation_log} for the admin FinOps dashboard. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinOpsService {

    private final AiGenerationLogRepository logRepository;

    @Transactional(readOnly = true)
    public AiUsageResponse getUsage(@Nullable Instant from, @Nullable Instant to) {
        List<Object[]> totalsRow = logRepository.findTotals(from, to);
        long totalRequests = 0;
        long totalPromptTokens = 0;
        long totalCompletionTokens = 0;
        BigDecimal totalCostUsd = null;

        if (!totalsRow.isEmpty()) {
            Object[] row = totalsRow.get(0);
            totalRequests = toLong(row[0]);
            totalPromptTokens = toLong(row[1]);
            totalCompletionTokens = toLong(row[2]);
            totalCostUsd = (BigDecimal) row[3];
        }

        List<AgentUsageSummary> byAgent = logRepository.findByAgent(from, to).stream()
                .map(FinOpsService::toAgentSummary)
                .toList();

        List<UserUsageSummary> byUser = logRepository.findByUser(from, to).stream()
                .map(FinOpsService::toUserSummary)
                .toList();

        List<CourseUsageSummary> byCourse = logRepository.findByCourse(from, to).stream()
                .map(FinOpsService::toCourseSummary)
                .toList();

        return new AiUsageResponse(
                from,
                to,
                totalRequests,
                totalPromptTokens,
                totalCompletionTokens,
                totalCostUsd,
                byAgent,
                byUser,
                byCourse);
    }

    private static AgentUsageSummary toAgentSummary(Object[] row) {
        AgentType agent = (AgentType) row[0];
        long count = toLong(row[1]);
        long promptTokens = toLong(row[2]);
        long completionTokens = toLong(row[3]);
        BigDecimal costUsd = (BigDecimal) row[4];
        Double avgLatencyRaw = (Double) row[5];
        Long avgLatencyMs = avgLatencyRaw != null ? avgLatencyRaw.longValue() : null;
        return new AgentUsageSummary(agent, count, promptTokens, completionTokens, costUsd, avgLatencyMs);
    }

    private static UserUsageSummary toUserSummary(Object[] row) {
        UUID userId = (UUID) row[0];
        String email = (String) row[1];
        long count = toLong(row[2]);
        BigDecimal costUsd = (BigDecimal) row[3];
        return new UserUsageSummary(userId, email, count, costUsd);
    }

    private static CourseUsageSummary toCourseSummary(Object[] row) {
        UUID courseId = (UUID) row[0];
        String title = (String) row[1];
        long count = toLong(row[2]);
        BigDecimal costUsd = (BigDecimal) row[3];
        return new CourseUsageSummary(courseId, title, count, costUsd);
    }

    private static long toLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
