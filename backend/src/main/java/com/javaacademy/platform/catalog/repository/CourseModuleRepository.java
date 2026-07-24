package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.CourseModule;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {}
