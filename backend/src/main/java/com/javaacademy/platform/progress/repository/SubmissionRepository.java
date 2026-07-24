package com.javaacademy.platform.progress.repository;

import com.javaacademy.platform.progress.entity.Submission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {}
