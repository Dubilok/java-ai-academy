package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.CourseModule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findByCourseOrderByOrderIndexAsc(Course course);

    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM CourseModule m WHERE m.course.id = :courseId")
    int findMaxOrderIndexByCourseId(@Param("courseId") UUID courseId);
}
