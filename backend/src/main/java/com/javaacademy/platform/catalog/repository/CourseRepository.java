package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Course;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.createdAt ASC, c.id ASC")
    List<Course> findAllPublished(Pageable pageable);

    @Query("SELECT c FROM Course c ORDER BY c.createdAt DESC, c.id ASC")
    List<Course> findAllOrderByCreatedAtDesc();

    @Query("SELECT c FROM Course c WHERE c.isPublished = true "
            + "AND (c.createdAt > :afterTime OR (c.createdAt = :afterTime AND c.id > :afterId)) "
            + "ORDER BY c.createdAt ASC, c.id ASC")
    List<Course> findPublishedAfterCursor(
            @Param("afterTime") Instant afterTime, @Param("afterId") UUID afterId, Pageable pageable);
}
