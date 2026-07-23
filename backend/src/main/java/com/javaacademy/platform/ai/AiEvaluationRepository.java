package com.javaacademy.platform.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {}
