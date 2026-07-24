package com.javaacademy.platform.ai.repository;

import com.javaacademy.platform.ai.entity.AiHint;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiHintRepository extends JpaRepository<AiHint, UUID> {}
