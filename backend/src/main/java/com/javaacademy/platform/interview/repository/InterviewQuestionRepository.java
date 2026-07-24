package com.javaacademy.platform.interview.repository;

import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    @Query("SELECT q FROM InterviewQuestion q WHERE "
            + "(:technology IS NULL OR q.technology = :technology) AND "
            + "(:category IS NULL OR q.category = :category) AND "
            + "(:difficulty IS NULL OR q.difficulty = :difficulty) "
            + "ORDER BY q.technology, q.category, q.difficulty")
    List<InterviewQuestion> findByFilters(
            @Param("technology") @Nullable String technology,
            @Param("category") @Nullable String category,
            @Param("difficulty") @Nullable InterviewDifficulty difficulty);

    List<InterviewQuestion> findByTechnology(String technology);

    @Query("SELECT q FROM InterviewQuestion q WHERE q.technology = :technology AND q.id NOT IN :excludedIds")
    List<InterviewQuestion> findByTechnologyExcluding(
            @Param("technology") String technology, @Param("excludedIds") Collection<UUID> excludedIds);
}
