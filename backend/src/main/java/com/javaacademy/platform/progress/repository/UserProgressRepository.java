package com.javaacademy.platform.progress.repository;

import com.javaacademy.platform.progress.entity.UserProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {}
