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

    /** Carries both the course ID and the created lecture ID back to the caller. */
    public record ImportResult(UUID courseId, UUID lectureId) {}

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LectureRepository lectureRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    // ── Primitive builders (used by the curriculum generation loop) ───────────

    /** Creates an empty, unpublished course shell. */
    @Transactional
    public UUID createCourse(String technology, String courseName, String description) {
        Course course = new Course();
        course.setTitle(courseName);
        course.setDescription(description);
        course.setTechnology(technology);
        course.setPublished(false);
        course.setCreatedAt(Instant.now(clock));
        return courseRepository.save(course).getId();
    }

    /** Creates an empty module inside an existing course. */
    @Transactional
    public UUID createModule(UUID courseId, String moduleName, int orderIndex) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(moduleName);
        module.setOrderIndex(orderIndex);
        return moduleRepository.save(module).getId();
    }

    /**
     * Creates a lecture + first task in an existing module.
     * Returns both the course ID and the created lecture ID.
     */
    @Transactional
    public ImportResult importLectureAndTask(UUID moduleId, GeneratedContent content) {
        CourseModule module = moduleRepository
                .findById(moduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module not found: " + moduleId));

        Lecture lecture = saveLecture(module, content);
        saveTask(lecture, content);
        return new ImportResult(module.getCourse().getId(), lecture.getId());
    }

    /**
     * Creates a lecture + first task at an explicit order index.
     * Use this in parallel generation to avoid the race on findMaxOrderIndex + insert.
     */
    @Transactional
    public ImportResult importLectureAndTaskAtIndex(UUID moduleId, GeneratedContent content, int lectureOrderIndex) {
        CourseModule module = moduleRepository
                .findById(moduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module not found: " + moduleId));
        Lecture lecture = new Lecture();
        lecture.setModule(module);
        lecture.setTitle(content.lecture().title());
        lecture.setContentMarkdown(content.lecture().contentMarkdown());
        lecture.setOrderIndex(lectureOrderIndex);
        Lecture savedLecture = lectureRepository.save(lecture);
        saveTask(savedLecture, content);
        return new ImportResult(module.getCourse().getId(), savedLecture.getId());
    }

    /** Adds an extra task to an existing lecture (used for taskCount > 1). */
    @Transactional
    public void importAdditionalTask(UUID lectureId, GeneratedContent content) {
        Lecture lecture = lectureRepository
                .findById(lectureId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));
        saveTask(lecture, content);
    }

    // ── Legacy composite methods (kept for backward compatibility) ────────────

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

    // ── Private helpers ───────────────────────────────────────────────────────

    private void addLectureAndTask(CourseModule module, GeneratedContent content) {
        Lecture lecture = saveLecture(module, content);
        saveTask(lecture, content);
    }

    private Lecture saveLecture(CourseModule module, GeneratedContent content) {
        int nextLectureIndex = lectureRepository.findMaxOrderIndexByModuleId(module.getId()) + 1;
        Lecture lecture = new Lecture();
        lecture.setModule(module);
        lecture.setTitle(content.lecture().title());
        lecture.setContentMarkdown(content.lecture().contentMarkdown());
        lecture.setOrderIndex(nextLectureIndex);
        return lectureRepository.save(lecture);
    }

    private void saveTask(Lecture lecture, GeneratedContent content) {
        Task task = new Task();
        task.setLecture(lecture);
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
