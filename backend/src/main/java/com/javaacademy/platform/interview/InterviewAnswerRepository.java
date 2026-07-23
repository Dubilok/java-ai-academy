package com.javaacademy.platform.interview;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {}
