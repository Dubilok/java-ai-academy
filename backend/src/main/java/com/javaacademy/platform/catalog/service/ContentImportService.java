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
import com.javaacademy.platform.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * Creates a brand-new unpublished course with one module, lecture, and task.
     * Returns the new course ID.
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

        addLectureAndTask(savedModule, content);
        return savedCourse.getId();
    }

    /**
     * Adds a new module (with a lecture and task) to an existing course.
     * Returns the course ID.
     */
    @Transactional
    public UUID importIntoNewModule(UUID courseId, String moduleName, GeneratedContent content) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));

        int nextIndex = moduleRepository.findMaxOrderIndexByCourseId(courseId) + 1;

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(moduleName);
        module.setOrderIndex(nextIndex);
        CourseModule savedModule = moduleRepository.save(module);

        addLectureAndTask(savedModule, content);
        return courseId;
    }

    /**
     * Adds a lecture and task to an existing module.
     * Returns the course ID.
     */
    @Transactional
    public UUID importIntoModule(UUID moduleId, GeneratedContent content) {
        CourseModule module = moduleRepository
                .findById(moduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module not found: " + moduleId));

        addLectureAndTask(module, content);
        return module.getCourse().getId();
    }

    private void addLectureAndTask(CourseModule module, GeneratedContent content) {
        int nextLectureIndex = lectureRepository.findMaxOrderIndexByModuleId(module.getId()) + 1;

        Lecture lecture = new Lecture();
        lecture.setModule(module);
        lecture.setTitle(content.lecture().title());
        lecture.setContentMarkdown(content.lecture().contentMarkdown());
        lecture.setOrderIndex(nextLectureIndex);
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
    }
}
