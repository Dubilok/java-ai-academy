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
import com.javaacademy.platform.progress.enums.ProgressStatus;
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
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserProgressRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

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
        UserProgress saved = savedProgress(user, task, ProgressStatus.IN_PROGRESS, 1);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
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
        savedProgress(other, task, ProgressStatus.PASSED, 2);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndTaskId_whenDifferentTask_returnsEmpty() {
        Task otherTask = savedTask();
        savedProgress(user, otherTask, ProgressStatus.IN_PROGRESS, 1);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isEmpty();
    }

    // ── UNIQUE constraint (user_id, task_id) ──────────────────────────────────

    @Test
    void save_duplicateUserAndTask_throwsDataIntegrityViolation() {
        savedProgress(user, task, ProgressStatus.IN_PROGRESS, 1);

        UserProgress duplicate = progressFor(user, task, ProgressStatus.PASSED, 1);
        assertThatThrownBy(() -> userProgressRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── cascade delete ────────────────────────────────────────────────────────

    @Test
    void deleteUser_cascadesToUserProgress() {
        savedProgress(user, task, ProgressStatus.IN_PROGRESS, 3);

        userRepository.delete(user);
        userRepository.flush();

        assertThat(userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId()))
                .isEmpty();
    }

    // ── field persistence ─────────────────────────────────────────────────────

    @Test
    void save_allFields_persistedAndRetrievedCorrectly() {
        Instant now = Instant.parse("2026-07-24T12:00:00Z");
        UserProgress p = progressFor(user, task, ProgressStatus.PASSED, 5);
        p.setSubmittedCode("System.out.println(\"hi\");");
        p.setUpdatedAt(now);
        userProgressRepository.saveAndFlush(p);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ProgressStatus.PASSED);
        assertThat(found.get().getAttempts()).isEqualTo(5);
        assertThat(found.get().getSubmittedCode()).isEqualTo("System.out.println(\"hi\");");
        assertThat(found.get().getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void save_nullSubmittedCode_allowed() {
        UserProgress p = progressFor(user, task, ProgressStatus.IN_PROGRESS, 1);
        p.setSubmittedCode(null);
        userProgressRepository.saveAndFlush(p);

        Optional<UserProgress> found = userProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSubmittedCode()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserProgress savedProgress(User user, Task task, ProgressStatus status, int attempts) {
        UserProgress progress = progressFor(user, task, status, attempts);
        return userProgressRepository.saveAndFlush(progress);
    }

    private UserProgress progressFor(User user, Task task, ProgressStatus status, int attempts) {
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setTask(task);
        progress.setStatus(status);
        progress.setAttempts(attempts);
        progress.setUpdatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return progress;
    }

    private User savedUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$12$hash");
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
        return userRepository.saveAndFlush(user);
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

        Task task = new Task();
        task.setLecture(lecture);
        task.setTitle("Task 1");
        task.setDifficulty("EASY");
        task.setXpReward(10L);
        return taskRepository.saveAndFlush(task);
    }
}
