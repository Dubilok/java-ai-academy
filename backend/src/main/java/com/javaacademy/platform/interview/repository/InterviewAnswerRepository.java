package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {

    @Query("SELECT a.question.id FROM InterviewAnswer a WHERE a.session.id = :sessionId")
    List<UUID> findAskedQuestionIdsBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT a.score FROM InterviewAnswer a WHERE a.session.id = :sessionId ORDER BY a.createdAt ASC")
    List<Integer> findScoresBySessionId(@Param("sessionId") UUID sessionId);
}
