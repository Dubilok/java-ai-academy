package com.javaacademy.platform.ai.repository;

import com.javaacademy.platform.ai.entity.AiEvaluation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {

    List<AiEvaluation> findTop50ByOrderByCreatedAtDesc();

    List<AiEvaluation> findByTargetTypeOrderByCreatedAtDesc(String targetType);
}
