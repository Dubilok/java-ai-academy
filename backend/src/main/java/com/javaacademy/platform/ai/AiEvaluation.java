package com.javaacademy.platform.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@Entity
@Table(name = "ai_evaluation")
public class AiEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Nullable
    @Column(name = "target_id")
    private UUID targetId;

    @Nullable
    @Column(name = "judge_scores_json")
    private String judgeScoresJson;

    @Nullable
    @Column
    private String flags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
