package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewQuestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {}
