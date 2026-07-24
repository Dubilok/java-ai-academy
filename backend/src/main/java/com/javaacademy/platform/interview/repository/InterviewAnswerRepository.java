package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewAnswer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {}
