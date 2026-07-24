package com.javaacademy.platform.ai.repository;

import com.javaacademy.platform.ai.entity.AiGenerationLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {}
