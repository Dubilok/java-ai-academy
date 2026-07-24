package com.javaacademy.platform.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.CourseModule;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.progress.entity.UserProgress;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserProgressRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    UserProgressRepository userProgressRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CourseModuleRepository moduleRepository;

    @Autowired
    LectureRepository lectureRepository;

    @Autowired
    TaskRepository taskRepository;

    User user;
    Task task;

    @BeforeEach
    void setUp() {
        user = savedUser("student@example.com");
        task = savedTask();
    }

    // ── findByUserIdAndTaskId ─────────────────────────────────────────────────

    @Test
    void findByUserIdAndTaskId_whenProgressExists_returnsIt() {
        UserProgress saved = savedProgress(user, task, "IN_PROGRESS", 1);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(found.get().getAttempts()).isEqualTo(1);
    }

    @Test
    void findByUserIdAndTaskId_whenNoRow_returnsEmpty() {
        Optional<UserProgress> found =
                userProgressRepository.findByUserIdAndTaskId(UUID.randomUUID(), UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndTaskId_whenDifferentUser_returnsEmpty() {
        User other = savedUser("other@example.com");
        savedProgress(other, task, "PASSED", 2);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndTaskId_whenDifferentTask_returnsEmpty() {
        Task otherTask = savedTask();
        savedProgress(user, otherTask, "IN_PROGRESS", 1);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isEmpty();
    }

    // ── UNIQUE constraint (user_id, task_id) ──────────────────────────────────

    @Test
    void save_duplicateUserAndTask_throwsDataIntegrityViolation() {
        savedProgress(user, task, "IN_PROGRESS", 1);

        UserProgress duplicate = progressFor(user, task, "PASSED", 1);
        assertThatThrownBy(() -> userProgressRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── cascade delete ────────────────────────────────────────────────────────

    @Test
    void deleteUser_cascadesToUserProgress() {
        savedProgress(user, task, "IN_PROGRESS", 3);

        userRepository.delete(user);
        userRepository.flush();

        assertThat(userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId()))
                .isEmpty();
    }

    // ── field persistence ─────────────────────────────────────────────────────

    @Test
    void save_allFields_persistedAndRetrievedCorrectly() {
        Instant now = Instant.parse("2026-07-24T12:00:00Z");
        UserProgress p = progressFor(user, task, "PASSED", 5);
        p.setSubmittedCode("System.out.println(\"hi\");");
        p.setUpdatedAt(now);
        userProgressRepository.saveAndFlush(p);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("PASSED");
        assertThat(found.get().getAttempts()).isEqualTo(5);
        assertThat(found.get().getSubmittedCode()).isEqualTo("System.out.println(\"hi\");");
        assertThat(found.get().getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void save_nullSubmittedCode_allowed() {
        UserProgress p = progressFor(user, task, "IN_PROGRESS", 1);
        p.setSubmittedCode(null);
        userProgressRepository.saveAndFlush(p);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSubmittedCode()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserProgress savedProgress(User u, Task t, String status, int attempts) {
        UserProgress p = progressFor(u, t, status, attempts);
        return userProgressRepository.saveAndFlush(p);
    }

    private UserProgress progressFor(User u, Task t, String status, int attempts) {
        UserProgress p = new UserProgress();
        p.setUser(u);
        p.setTask(t);
        p.setStatus(status);
        p.setAttempts(attempts);
        p.setUpdatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return p;
    }

    private User savedUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$hash");
        u.setRole("ROLE_STUDENT");
        u.setXpPoints(0L);
        u.setCrystals(0L);
        u.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return userRepository.saveAndFlush(u);
    }

    private Task savedTask() {
        Course course = new Course();
        course.setTitle("Java 21");
        course.setTechnology("Java");
        course.setPublished(false);
        course.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        courseRepository.saveAndFlush(course);

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setOrderIndex(1);
        moduleRepository.saveAndFlush(module);

        Lecture lecture = new Lecture();
        lecture.setModule(module);
        lecture.setTitle("Lecture 1");
        lecture.setOrderIndex(1);
        lectureRepository.saveAndFlush(lecture);

        Task t = new Task();
        t.setLecture(lecture);
        t.setTitle("Task 1");
        t.setDifficulty("EASY");
        t.setXpReward(10L);
        return taskRepository.saveAndFlush(t);
    }
}
