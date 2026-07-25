package com.javaacademy.platform.catalog.repository;

import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByLectureOrderByIdAsc(Lecture lecture);

    @Query("SELECT COUNT(task) FROM Task task WHERE task.lecture.module.course.id = :courseId")
    long countTasksInCourse(@Param("courseId") UUID courseId);

    @Query(
            "SELECT task FROM Task task WHERE task.lecture.module.course.id = :courseId ORDER BY task.lecture.module.orderIndex ASC, task.lecture.orderIndex ASC, task.id ASC")
    List<Task> findAllByCourseId(@Param("courseId") UUID courseId);
}
