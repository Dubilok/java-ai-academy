package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Optional<InterviewSession> findByIdAndUser_Id(UUID sessionId, UUID userId);

    List<InterviewSession> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
