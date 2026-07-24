package com.javaacademy.platform.catalog.service;

import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.CourseModule;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentImportService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LectureRepository lectureRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    /**
     * Persists AI-generated content as an unpublished course with one module, one lecture,
     * and one task. Returns the new course ID.
     */
    @Transactional
    public UUID importGenerated(String technology, GeneratedContent content) {
        Course course = new Course();
        course.setTitle(content.lecture().title());
        course.setDescription("AI-generated course for: " + technology);
        course.setTechnology(technology);
        course.setPublished(false);
        course.setCreatedAt(Instant.now(clock));
        Course savedCourse = courseRepository.save(course);

        CourseModule module = new CourseModule();
        module.setCourse(savedCourse);
        module.setTitle(technology);
        module.setOrderIndex(1);
        CourseModule savedModule = moduleRepository.save(module);

        Lecture lecture = new Lecture();
        lecture.setModule(savedModule);
        lecture.setTitle(content.lecture().title());
        lecture.setContentMarkdown(content.lecture().contentMarkdown());
        lecture.setOrderIndex(1);
        Lecture savedLecture = lectureRepository.save(lecture);

        Task task = new Task();
        task.setLecture(savedLecture);
        task.setTitle(content.task().title());
        task.setDescription(content.task().description());
        task.setDifficulty(content.task().difficulty().name());
        task.setTemplateCode(content.templateCode());
        task.setTestCode(content.testCode());
        task.setSolutionCode(content.solutionCode());
        task.setXpReward(content.task().xpReward());
        taskRepository.save(task);

        return savedCourse.getId();
    }
}
