package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Lecture;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, UUID> {}
