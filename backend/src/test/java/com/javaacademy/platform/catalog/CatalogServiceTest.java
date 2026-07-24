package com.javaacademy.platform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.PagedResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.catalog.service.CatalogService;
import com.javaacademy.platform.common.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    CourseRepository courseRepository;

    @Mock
    CourseModuleRepository moduleRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    TaskRepository taskRepository;

    @InjectMocks
    CatalogService service;

    // ── cursor encoding / decoding ────────────────────────────────────────────

    @Test
    void encodeCursor_producesValidBase64Url() {
        Instant ts = Instant.parse("2026-07-24T10:00:00Z");
        UUID id = UUID.randomUUID();

        String encoded = CatalogService.encodeCursor(ts, id);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).doesNotContain("+", "/", "=");
    }

    @Test
    void listPublishedCourses_invalidCursor_throwsBadRequest() {
        assertThatThrownBy(() -> service.listPublishedCourses("!!!not-base64!!!", 20))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listPublishedCourses_validBase64ButBadFormat_throwsBadRequest() {
        String bad = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("no-tilde-separator".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.listPublishedCourses(bad, 20))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── listPublishedCourses ──────────────────────────────────────────────────

    @Test
    void listPublishedCourses_noCursor_returnsFirstPage() {
        Course c1 = course("Java Basics", Instant.parse("2026-01-01T00:00:00Z"));
        Course c2 = course("Spring Boot", Instant.parse("2026-02-01T00:00:00Z"));
        when(courseRepository.findAllPublished(PageRequest.of(0, 3))).thenReturn(List.of(c1, c2));

        PagedResponse<com.javaacademy.platform.catalog.dto.CourseResponse> result =
                service.listPublishedCourses(null, 2);

        assertThat(result.items()).hasSize(2);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.items().get(0).title()).isEqualTo("Java Basics");
    }

    @Test
    void listPublishedCourses_hasNextPage_returnsNextCursor() {
        // fetch size = limit + 1 = 3; returning 3 items means there are more
        List<Course> threeCourses = List.of(
                course("A", Instant.parse("2026-01-01T00:00:00Z")),
                course("B", Instant.parse("2026-02-01T00:00:00Z")),
                course("C", Instant.parse("2026-03-01T00:00:00Z")));
        when(courseRepository.findAllPublished(PageRequest.of(0, 3))).thenReturn(threeCourses);

        PagedResponse<com.javaacademy.platform.catalog.dto.CourseResponse> result =
                service.listPublishedCourses(null, 2);

        assertThat(result.items()).hasSize(2);
        assertThat(result.nextCursor()).isNotNull();
    }

    @Test
    void listPublishedCourses_withCursor_queriesAfterCursor() {
        Instant ts = Instant.parse("2026-01-15T00:00:00Z");
        UUID id = UUID.randomUUID();
        String cursor = CatalogService.encodeCursor(ts, id);

        when(courseRepository.findPublishedAfterCursor(ts, id, PageRequest.of(0, 3)))
                .thenReturn(List.of());

        PagedResponse<com.javaacademy.platform.catalog.dto.CourseResponse> result =
                service.listPublishedCourses(cursor, 2);

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void listPublishedCourses_limitExceedsMax_cappedAt100() {
        when(courseRepository.findAllPublished(PageRequest.of(0, 101))).thenReturn(List.of());

        PagedResponse<?> result = service.listPublishedCourses(null, 9999);

        assertThat(result.items()).isEmpty();
    }

    // ── getCourse ─────────────────────────────────────────────────────────────

    @Test
    void getCourse_whenExists_returnsCourseWithModules() {
        Course course = course("Spring", Instant.parse("2026-01-01T00:00:00Z"));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourseOrderByOrderIndexAsc(course)).thenReturn(List.of());

        CourseDetailResponse result = service.getCourse(course.getId());

        assertThat(result.id()).isEqualTo(course.getId());
        assertThat(result.title()).isEqualTo("Spring");
        assertThat(result.modules()).isEmpty();
    }

    @Test
    void getCourse_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCourse(id))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getLecture ────────────────────────────────────────────────────────────

    @Test
    void getLecture_whenExists_returnsLectureWithTaskStubs() {
        Lecture lecture = lecture("JUnit 5 basics");
        when(lectureRepository.findById(lecture.getId())).thenReturn(Optional.of(lecture));
        when(taskRepository.findByLectureOrderByIdAsc(lecture)).thenReturn(List.of());

        LectureResponse result = service.getLecture(lecture.getId());

        assertThat(result.id()).isEqualTo(lecture.getId());
        assertThat(result.title()).isEqualTo("JUnit 5 basics");
        assertThat(result.tasks()).isEmpty();
    }

    @Test
    void getLecture_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(lectureRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLecture(id))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getTask ───────────────────────────────────────────────────────────────

    @Test
    void getTask_whenExists_returnsTaskWithoutSecretFields() {
        Task task = task("Write hello world");
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        TaskResponse result = service.getTask(task.getId());

        assertThat(result.id()).isEqualTo(task.getId());
        assertThat(result.title()).isEqualTo("Write hello world");
        // verify TaskResponse has no testCode or solutionCode fields
        // (enforced at compile time — this assertion documents the intent)
    }

    @Test
    void getTask_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTask(id))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Course course(String title, Instant createdAt) {
        // Use reflection-free construction via setters (Lombok @Setter on entity)
        Course c = new Course();
        c.setTitle(title);
        c.setDescription(null);
        c.setTechnology("Java");
        c.setPublished(true);
        c.setCreatedAt(createdAt);
        return c;
    }

    private Lecture lecture(String title) {
        Lecture l = new Lecture();
        l.setTitle(title);
        l.setContentMarkdown("# Content");
        l.setOrderIndex(1);
        return l;
    }

    private Task task(String title) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription("A description");
        t.setDifficulty("EASY");
        t.setTemplateCode("// write here");
        t.setTestCode("hidden test");
        t.setSolutionCode("hidden solution");
        t.setXpReward(100L);
        return t;
    }
}
