package com.javaacademy.platform.catalog.service;

import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.LectureStubResponse;
import com.javaacademy.platform.catalog.dto.ModuleResponse;
import com.javaacademy.platform.catalog.dto.PagedResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.dto.TaskStubResponse;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.CourseModule;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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
            CursorParts cursor = decodeCursor(cursorStr);
            courses = courseRepository.findPublishedAfterCursor(
                    cursor.createdAt(), cursor.id(), PageRequest.of(0, fetchSize));
        }

        boolean hasNext = courses.size() == fetchSize;
        List<Course> page = hasNext ? courses.subList(0, fetchSize - 1) : courses;

        String nextCursor = null;
        if (hasNext) {
            Course last = page.get(page.size() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
        }

        return new PagedResponse<>(page.stream().map(this::toCourseResponse).toList(), nextCursor);
    }

    public CourseDetailResponse getCourse(UUID id) {
        Course course = courseRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + id));

        List<ModuleResponse> modules = moduleRepository.findByCourseOrderByOrderIndexAsc(course).stream()
                .map(this::toModuleResponse)
                .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getTechnology(),
                course.getCreatedAt(),
                modules);
    }

    public LectureResponse getLecture(UUID id) {
        Lecture lecture = lectureRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lecture not found: " + id));

        List<TaskStubResponse> tasks = taskRepository.findByLectureOrderByIdAsc(lecture).stream()
                .map(t -> new TaskStubResponse(t.getId(), t.getTitle(), t.getDifficulty(), t.getXpReward()))
                .toList();

        return new LectureResponse(
                lecture.getId(), lecture.getTitle(), lecture.getContentMarkdown(), lecture.getOrderIndex(), tasks);
    }

    public TaskResponse getTask(UUID id) {
        Task task = taskRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDifficulty(),
                task.getTemplateCode(),
                task.getXpReward());
    }

    // ── cursor helpers ────────────────────────────────────────────────────────

    private record CursorParts(Instant createdAt, UUID id) {}

    public static String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toEpochMilli() + "~" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static CursorParts decodeCursor(String cursor) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split("~", 2);
            return new CursorParts(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private CourseResponse toCourseResponse(Course c) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getDescription(), c.getTechnology(), c.getCreatedAt());
    }

    private ModuleResponse toModuleResponse(CourseModule m) {
        List<LectureStubResponse> lectures = lectureRepository.findByModuleOrderByOrderIndexAsc(m).stream()
                .map(l -> new LectureStubResponse(l.getId(), l.getTitle(), l.getOrderIndex()))
                .toList();
        return new ModuleResponse(m.getId(), m.getTitle(), m.getOrderIndex(), lectures);
    }
}
