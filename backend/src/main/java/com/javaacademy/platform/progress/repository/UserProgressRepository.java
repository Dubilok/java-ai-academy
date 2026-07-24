package com.javaacademy.platform.progress.repository;

import com.javaacademy.platform.progress.entity.UserProgress;
import com.javaacademy.platform.progress.enums.ProgressStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.task.id = :taskId")
    Optional<UserProgress> findByUserIdAndTaskId(@Param("userId") UUID userId, @Param("taskId") UUID taskId);

    @Query("SELECT COUNT(up) FROM UserProgress up "
            + "WHERE up.user.id = :userId "
            + "AND up.task.lecture.module.course.id = :courseId "
            + "AND up.status = :status")
    long countByUserIdAndCourseIdAndStatus(
            @Param("userId") UUID userId, @Param("courseId") UUID courseId, @Param("status") ProgressStatus status);
}
