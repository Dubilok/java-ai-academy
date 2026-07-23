package com.javaacademy.platform.catalog;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, UUID> {}
