package com.javaacademy.platform.progress.repository;

import com.javaacademy.platform.progress.entity.UserProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.task.id = :taskId")
    Optional<UserProgress> findByUserIdAndTaskId(@Param("userId") UUID userId, @Param("taskId") UUID taskId);
}
