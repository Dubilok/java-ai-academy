package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.CourseModule;
import com.javaacademy.platform.catalog.entity.Lecture;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, UUID> {

    List<Lecture> findByModuleOrderByOrderIndexAsc(CourseModule module);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lecture l WHERE l.module.id = :moduleId")
    int findMaxOrderIndexByModuleId(@Param("moduleId") UUID moduleId);
}
