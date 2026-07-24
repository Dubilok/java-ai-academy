package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {}
