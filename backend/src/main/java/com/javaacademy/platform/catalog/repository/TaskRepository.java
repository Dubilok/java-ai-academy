package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Task;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {}
