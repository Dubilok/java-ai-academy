package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {}
