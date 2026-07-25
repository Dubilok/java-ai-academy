package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.dto.AgentUsageSummary;
import com.javaacademy.platform.ai.dto.AiUsageResponse;
import com.javaacademy.platform.ai.enums.AgentType;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinOpsServiceTest {

    AiGenerationLogRepository mockRepository;
    FinOpsService service;

    @BeforeEach
    void setUp() {
        mockRepository = mock(AiGenerationLogRepository.class);
        service = new FinOpsService(mockRepository);

        when(mockRepository.findTotals(any(), any())).thenReturn(List.of());
        when(mockRepository.findByAgent(any(), any())).thenReturn(List.of());
        when(mockRepository.findByUser(any(), any())).thenReturn(List.of());
        when(mockRepository.findByCourse(any(), any())).thenReturn(List.of());
    }

    @Test
    void getUsage_emptyLog_returnsZeroTotals() {
        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.totalRequests()).isZero();
        assertThat(response.totalPromptTokens()).isZero();
        assertThat(response.totalCompletionTokens()).isZero();
        assertThat(response.totalCostUsd()).isNull();
        assertThat(response.byAgent()).isEmpty();
        assertThat(response.byUser()).isEmpty();
        assertThat(response.byCourse()).isEmpty();
    }

    @Test
    void getUsage_withTotalsRow_returnsMappedTotals() {
        Object[] totalsRow = {3L, 1000L, 500L, new BigDecimal("4.50")};
        when(mockRepository.findTotals(any(), any())).thenReturn(List.<Object[]>of(totalsRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.totalRequests()).isEqualTo(3);
        assertThat(response.totalPromptTokens()).isEqualTo(1000);
        assertThat(response.totalCompletionTokens()).isEqualTo(500);
        assertThat(response.totalCostUsd()).isEqualByComparingTo("4.50");
    }

    @Test
    void getUsage_byAgentRow_returnsMappedSummary() {
        Object[] agentRow = {AgentType.CONTENT_ARCHITECT, 10L, 5000L, 2500L, new BigDecimal("7.50"), 350.0};
        when(mockRepository.findByAgent(any(), any())).thenReturn(List.<Object[]>of(agentRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.byAgent()).hasSize(1);
        AgentUsageSummary summary = response.byAgent().get(0);
        assertThat(summary.agent()).isEqualTo(AgentType.CONTENT_ARCHITECT);
        assertThat(summary.requestCount()).isEqualTo(10);
        assertThat(summary.promptTokens()).isEqualTo(5000);
        assertThat(summary.completionTokens()).isEqualTo(2500);
        assertThat(summary.costUsd()).isEqualByComparingTo("7.50");
        assertThat(summary.avgLatencyMs()).isEqualTo(350L);
    }

    @Test
    void getUsage_agentRowWithNullAvgLatency_handlesNull() {
        Object[] agentRow = {AgentType.SOCRATIC_MENTOR, 5L, 2000L, 1000L, new BigDecimal("1.20"), null};
        when(mockRepository.findByAgent(any(), any())).thenReturn(List.<Object[]>of(agentRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.byAgent().get(0).avgLatencyMs()).isNull();
    }

    @Test
    void getUsage_byUserRow_returnsMappedSummary() {
        UUID userId = UUID.randomUUID();
        Object[] userRow = {userId, "student@example.com", 4L, new BigDecimal("0.80")};
        when(mockRepository.findByUser(any(), any())).thenReturn(List.<Object[]>of(userRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.byUser()).hasSize(1);
        assertThat(response.byUser().get(0).userId()).isEqualTo(userId);
        assertThat(response.byUser().get(0).email()).isEqualTo("student@example.com");
        assertThat(response.byUser().get(0).requestCount()).isEqualTo(4);
        assertThat(response.byUser().get(0).costUsd()).isEqualByComparingTo("0.80");
    }

    @Test
    void getUsage_byCourseRow_returnsMappedSummary() {
        UUID courseId = UUID.randomUUID();
        Object[] courseRow = {courseId, "Java 21 Fundamentals", 20L, new BigDecimal("6.00")};
        when(mockRepository.findByCourse(any(), any())).thenReturn(List.<Object[]>of(courseRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.byCourse()).hasSize(1);
        assertThat(response.byCourse().get(0).courseId()).isEqualTo(courseId);
        assertThat(response.byCourse().get(0).title()).isEqualTo("Java 21 Fundamentals");
        assertThat(response.byCourse().get(0).requestCount()).isEqualTo(20);
    }

    @Test
    void getUsage_fromToPassedThrough_appearsInResponse() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-31T23:59:59Z");

        AiUsageResponse response = service.getUsage(from, to);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
    }

    @Test
    void getUsage_nullCostInTotals_totalCostUsdIsNull() {
        Object[] totalsRow = {0L, 0L, 0L, null};
        when(mockRepository.findTotals(any(), any())).thenReturn(List.<Object[]>of(totalsRow));

        AiUsageResponse response = service.getUsage(null, null);

        assertThat(response.totalCostUsd()).isNull();
    }
}
