package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.VoiceInterviewQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoiceInterviewQuestionRepository extends JpaRepository<VoiceInterviewQuestion, UUID> {

    @Query("SELECT v FROM VoiceInterviewQuestion v WHERE v.session.id = :sessionId ORDER BY v.turnIndex ASC")
    List<VoiceInterviewQuestion> findBySessionIdOrderByTurnIndex(@Param("sessionId") UUID sessionId);
}
