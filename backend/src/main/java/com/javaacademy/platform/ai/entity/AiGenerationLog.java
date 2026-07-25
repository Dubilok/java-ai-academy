package com.javaacademy.platform.ai.entity;

import com.javaacademy.platform.ai.enums.AgentType;
import com.javaacademy.platform.ai.enums.GenerationOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@Entity
@Table(name = "ai_generation_log")
public class AiGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentType agent;

    @Column(nullable = false)
    private String model;

    @Nullable
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Nullable
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Nullable
    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Nullable
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GenerationOutcome outcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Nullable
    @Column(name = "user_id")
    private UUID userId;

    @Nullable
    @Column(name = "course_id")
    private UUID courseId;
}
