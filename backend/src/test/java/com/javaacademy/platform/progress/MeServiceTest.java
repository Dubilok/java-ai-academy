package com.javaacademy.platform.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.progress.dto.CourseProgressResponse;
import com.javaacademy.platform.progress.dto.MeResponse;
import com.javaacademy.platform.progress.dto.ProgressSummaryResponse;
import com.javaacademy.platform.progress.enums.ProgressStatus;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import com.javaacademy.platform.progress.service.MeService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    CourseRepository courseRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserProgressRepository userProgressRepository;

    @InjectMocks
    MeService service;

    static final String EMAIL = "student@test.com";
    static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");

    // ── getMe ─────────────────────────────────────────────────────────────────

    @Test
    void getMe_returnsProfileWithComputedLevel() {
        User user = userWithXp(400L); // level 3 per XpCalculator formula
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        MeResponse response = service.getMe(EMAIL);

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.xpPoints()).isEqualTo(400L);
        assertThat(response.level()).isEqualTo(3);
        assertThat(response.crystals()).isEqualTo(2L);
        assertThat(response.streak()).isEqualTo(0);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void getMe_whenUserNotFound_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe(EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getProgress ───────────────────────────────────────────────────────────

    @Test
    void getProgress_noCourses_returnsEmptyList() {
        User user = userWithXp(0L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(courseRepository.findAllPublished(Pageable.unpaged())).thenReturn(List.of());

        ProgressSummaryResponse response = service.getProgress(EMAIL);

        assertThat(response.items()).isEmpty();
    }

    @Test
    void getProgress_oneCourseNoAttempts_returns0Percent() {
        User user = userWithXp(0L);
        Course course = publishedCourse("Java Basics");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(courseRepository.findAllPublished(Pageable.unpaged())).thenReturn(List.of(course));
        when(taskRepository.countTasksInCourse(course.getId())).thenReturn(5L);
        when(userProgressRepository.countByUserIdAndCourseIdAndStatus(
                        user.getId(), course.getId(), ProgressStatus.PASSED))
                .thenReturn(0L);

        ProgressSummaryResponse response = service.getProgress(EMAIL);

        CourseProgressResponse item = response.items().get(0);
        assertThat(item.totalTasks()).isEqualTo(5L);
        assertThat(item.passedTasks()).isEqualTo(0L);
        assertThat(item.completionPercent()).isEqualTo(0);
    }

    @Test
    void getProgress_partialCompletion_computesPercentCorrectly() {
        User user = userWithXp(0L);
        Course course = publishedCourse("Spring Boot");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(courseRepository.findAllPublished(Pageable.unpaged())).thenReturn(List.of(course));
        when(taskRepository.countTasksInCourse(course.getId())).thenReturn(10L);
        when(userProgressRepository.countByUserIdAndCourseIdAndStatus(
                        user.getId(), course.getId(), ProgressStatus.PASSED))
                .thenReturn(3L);

        ProgressSummaryResponse response = service.getProgress(EMAIL);

        assertThat(response.items().get(0).completionPercent()).isEqualTo(30);
    }

    @Test
    void getProgress_allTasksPassed_returns100Percent() {
        User user = userWithXp(0L);
        Course course = publishedCourse("Spring Boot");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(courseRepository.findAllPublished(Pageable.unpaged())).thenReturn(List.of(course));
        when(taskRepository.countTasksInCourse(course.getId())).thenReturn(4L);
        when(userProgressRepository.countByUserIdAndCourseIdAndStatus(
                        user.getId(), course.getId(), ProgressStatus.PASSED))
                .thenReturn(4L);

        ProgressSummaryResponse response = service.getProgress(EMAIL);

        assertThat(response.items().get(0).completionPercent()).isEqualTo(100);
    }

    @Test
    void getProgress_courseWithNoTasks_returns0Percent() {
        User user = userWithXp(0L);
        Course course = publishedCourse("Empty Course");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(courseRepository.findAllPublished(Pageable.unpaged())).thenReturn(List.of(course));
        when(taskRepository.countTasksInCourse(course.getId())).thenReturn(0L);
        when(userProgressRepository.countByUserIdAndCourseIdAndStatus(
                        user.getId(), course.getId(), ProgressStatus.PASSED))
                .thenReturn(0L);

        ProgressSummaryResponse response = service.getProgress(EMAIL);

        assertThat(response.items().get(0).completionPercent()).isEqualTo(0);
    }

    @Test
    void getProgress_whenUserNotFound_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithXp(long xpPoints) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$12$hash");
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(xpPoints);
        user.setCrystals(2L);
        user.setCreatedAt(CREATED_AT);
        return user;
    }

    private Course publishedCourse(String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setTechnology("Java");
        course.setPublished(true);
        course.setCreatedAt(CREATED_AT);
        return course;
    }
}
