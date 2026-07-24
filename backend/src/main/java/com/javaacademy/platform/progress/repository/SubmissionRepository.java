package com.javaacademy.platform.progress.repository;

import com.javaacademy.platform.progress.entity.Submission;
import com.javaacademy.platform.progress.enums.SubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<Submission> findTopByUser_IdAndTask_IdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID taskId, SubmissionStatus status);
}
