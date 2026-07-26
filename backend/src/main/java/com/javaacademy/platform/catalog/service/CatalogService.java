package com.javaacademy.platform.catalog.service;

import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.IdeBootstrapCourseResponse;
import com.javaacademy.platform.catalog.dto.IdeBootstrapTaskResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.LectureStubResponse;
import com.javaacademy.platform.catalog.dto.ModuleResponse;
import com.javaacademy.platform.catalog.dto.PagedResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.dto.TaskStubResponse;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.mapper.CatalogMapper;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.catalog.util.CursorEncoder;
import com.javaacademy.platform.common.ApiException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    static final int MAX_LIMIT = 100;
    static final int DEFAULT_LIMIT = 20;

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LectureRepository lectureRepository;
    private final TaskRepository taskRepository;

    public PagedResponse<CourseResponse> listPublishedCourses(@Nullable String cursorStr, int limit) {
        int fetchSize = Math.min(limit, MAX_LIMIT) + 1;

        List<Course> courses;
        if (cursorStr == null) {
            courses = courseRepository.findAllPublished(PageRequest.of(0, fetchSize));
        } else {
            CursorEncoder.CursorParts cursor = CursorEncoder.decode(cursorStr);
            courses = courseRepository.findPublishedAfterCursor(
                    cursor.createdAt(), cursor.id(), PageRequest.of(0, fetchSize));
        }

        boolean hasNext = courses.size() == fetchSize;
        List<Course> page = hasNext ? courses.subList(0, fetchSize - 1) : courses;

        String nextCursor = null;
        if (hasNext) {
            Course last = page.get(page.size() - 1);
            nextCursor = CursorEncoder.encode(last.getCreatedAt(), last.getId());
        }

        return new PagedResponse<>(
                page.stream().map(CatalogMapper::toCourseResponse).toList(), nextCursor);
    }

    public CourseDetailResponse getCourse(UUID id) {
        Course course = courseRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + id));

        List<ModuleResponse> modules = moduleRepository.findByCourseOrderByOrderIndexAsc(course).stream()
                .map(module -> {
                    List<LectureStubResponse> lectureStubs =
                            lectureRepository.findByModuleOrderByOrderIndexAsc(module).stream()
                                    .map(lecture -> {
                                        List<TaskStubResponse> taskStubs =
                                                taskRepository.findByLectureOrderByIdAsc(lecture).stream()
                                                        .map(CatalogMapper::toTaskStubResponse)
                                                        .toList();
                                        return CatalogMapper.toLectureStubResponse(lecture, taskStubs);
                                    })
                                    .toList();
                    return CatalogMapper.toModuleResponse(module, lectureStubs);
                })
                .toList();

        return CatalogMapper.toCourseDetailResponse(course, modules);
    }

    public LectureResponse getLecture(UUID id) {
        Lecture lecture = lectureRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lecture not found: " + id));

        List<TaskStubResponse> taskStubs = taskRepository.findByLectureOrderByIdAsc(lecture).stream()
                .map(CatalogMapper::toTaskStubResponse)
                .toList();

        return CatalogMapper.toLectureResponse(lecture, taskStubs);
    }

    public TaskResponse getTask(UUID id) {
        Task task = taskRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        return CatalogMapper.toTaskResponse(task);
    }

    /** All courses (published + unpublished) for admin pickers, newest first. */
    public List<CourseResponse> listAllCourses() {
        return courseRepository.findAllOrderByCreatedAtDesc().stream()
                .map(CatalogMapper::toCourseResponse)
                .toList();
    }

    /** Modules for a course, ordered by orderIndex. */
    public List<ModuleResponse> listModulesForCourse(UUID courseId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));
        return moduleRepository.findByCourseOrderByOrderIndexAsc(course).stream()
                .map(module -> CatalogMapper.toModuleResponse(module, List.of()))
                .toList();
    }

    /** Toggles the published flag on a course and returns the updated response. */
    @Transactional
    public CourseResponse togglePublish(UUID courseId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));
        course.setPublished(!course.isPublished());
        return CatalogMapper.toCourseResponse(course);
    }

    public List<IdeBootstrapCourseResponse> getIdeBootstrap() {
        return courseRepository.findAllPublished(PageRequest.of(0, MAX_LIMIT)).stream()
                .map(course -> {
                    List<IdeBootstrapTaskResponse> tasks = taskRepository.findAllByCourseId(course.getId()).stream()
                            .map(task -> new IdeBootstrapTaskResponse(
                                    task.getId(), task.getTitle(), task.getDifficulty(), task.getXpReward()))
                            .toList();
                    return new IdeBootstrapCourseResponse(
                            course.getId(), course.getTitle(), course.getTechnology(), tasks);
                })
                .toList();
    }
}
