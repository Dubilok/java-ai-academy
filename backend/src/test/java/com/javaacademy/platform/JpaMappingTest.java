package com.javaacademy.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.ai.entity.AiEvaluation;
import com.javaacademy.platform.ai.entity.AiGenerationLog;
import com.javaacademy.platform.ai.repository.AiEvaluationRepository;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
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
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.entity.InterviewSession;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import com.javaacademy.platform.interview.repository.InterviewSessionRepository;
import com.javaacademy.platform.progress.entity.Submission;
import com.javaacademy.platform.progress.entity.UserProgress;
import com.javaacademy.platform.progress.enums.ProgressStatus;
import com.javaacademy.platform.progress.enums.SubmissionStatus;
import com.javaacademy.platform.progress.repository.SubmissionRepository;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers
@Transactional
class JpaMappingTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CourseModuleRepository courseModuleRepository;

    @Autowired
    LectureRepository lectureRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserProgressRepository userProgressRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    InterviewSessionRepository interviewSessionRepository;

    @Autowired
    InterviewAnswerRepository interviewAnswerRepository;

    @Autowired
    AiGenerationLogRepository aiGenerationLogRepository;

    @Autowired
    AiEvaluationRepository aiEvaluationRepository;

    @Test
    void user_persistsAndLoadsById() {
        User saved = userRepository.save(makeUser("user@test.com"));
        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void course_persistsAndLoadsById() {
        Course saved = courseRepository.save(makeCourse("Java"));
        assertThat(saved.getId()).isNotNull();
        assertThat(courseRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void courseModule_persistsAndLoadsById() {
        Course course = courseRepository.save(makeCourse("Java"));
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle("Collections");
        module.setOrderIndex(1);
        CourseModule saved = courseModuleRepository.save(module);
        assertThat(saved.getId()).isNotNull();
        assertThat(courseModuleRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void lecture_persistsAndLoadsById() {
        Lecture saved = lectureRepository.save(makeLecture());
        assertThat(saved.getId()).isNotNull();
        assertThat(lectureRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void task_persistsAndLoadsById() {
        Task saved = taskRepository.save(makeTask());
        assertThat(saved.getId()).isNotNull();
        assertThat(taskRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void userProgress_persistsAndLoadsById() {
        User user = userRepository.save(makeUser("prog@test.com"));
        Task task = taskRepository.save(makeTask());
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setTask(task);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setAttempts(0);
        progress.setUpdatedAt(Instant.now());
        UserProgress saved = userProgressRepository.save(progress);
        assertThat(saved.getId()).isNotNull();
        assertThat(userProgressRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void submission_persistsAndLoadsById() {
        User user = userRepository.save(makeUser("sub@test.com"));
        Task task = taskRepository.save(makeTask());
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setTask(task);
        submission.setSource("public class Solution {}");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setCreatedAt(Instant.now());
        Submission saved = submissionRepository.save(submission);
        assertThat(saved.getId()).isNotNull();
        assertThat(submissionRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void submission_withNullTask_persistsAndLoadsById() {
        User user = userRepository.save(makeUser("sub2@test.com"));
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setTask(null);
        submission.setSource("public class Solution {}");
        submission.setStatus(SubmissionStatus.PASSED);
        submission.setCreatedAt(Instant.now());
        Submission saved = submissionRepository.save(submission);
        assertThat(saved.getId()).isNotNull();
        assertThat(submissionRepository.findById(saved.getId()).orElseThrow().getTask())
                .isNull();
    }

    @Test
    void interviewQuestion_persistsAndLoadsById() {
        InterviewQuestion question = new InterviewQuestion();
        question.setTechnology("Java");
        question.setCategory("Collections");
        question.setQuestion("What is the difference between List and Set?");
        question.setDifficulty("INTERMEDIATE");
        InterviewQuestion saved = interviewQuestionRepository.save(question);
        assertThat(saved.getId()).isNotNull();
        assertThat(interviewQuestionRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void interviewSession_persistsAndLoadsById() {
        User user = userRepository.save(makeUser("sess@test.com"));
        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setTechnology("Java");
        session.setCreatedAt(Instant.now());
        InterviewSession saved = interviewSessionRepository.save(session);
        assertThat(saved.getId()).isNotNull();
        assertThat(interviewSessionRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void interviewAnswer_persistsAndLoadsById() {
        User user = userRepository.save(makeUser("ans@test.com"));
        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setTechnology("Java");
        session.setCreatedAt(Instant.now());
        InterviewSession savedSession = interviewSessionRepository.save(session);

        InterviewQuestion question = new InterviewQuestion();
        question.setTechnology("Java");
        question.setCategory("OOP");
        question.setQuestion("What is polymorphism?");
        question.setDifficulty("BEGINNER");
        InterviewQuestion savedQuestion = interviewQuestionRepository.save(question);

        InterviewAnswer answer = new InterviewAnswer();
        answer.setSession(savedSession);
        answer.setQuestion(savedQuestion);
        answer.setAnswerText("The ability of different objects to respond to the same message.");
        InterviewAnswer saved = interviewAnswerRepository.save(answer);
        assertThat(saved.getId()).isNotNull();
        assertThat(interviewAnswerRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void aiGenerationLog_persistsAndLoadsById() {
        AiGenerationLog log = new AiGenerationLog();
        log.setAgent(com.javaacademy.platform.ai.enums.AgentType.CONTENT_ARCHITECT);
        log.setModel("claude-sonnet-4-6");
        log.setOutcome(com.javaacademy.platform.ai.enums.GenerationOutcome.SUCCEEDED);
        log.setCreatedAt(Instant.now());
        AiGenerationLog saved = aiGenerationLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(aiGenerationLogRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void aiEvaluation_persistsAndLoadsById() {
        AiEvaluation eval = new AiEvaluation();
        eval.setTargetType("hint");
        eval.setCreatedAt(Instant.now());
        AiEvaluation saved = aiEvaluationRepository.save(eval);
        assertThat(saved.getId()).isNotNull();
        assertThat(aiEvaluationRepository.findById(saved.getId())).isPresent();
    }

    private User makeUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$12$placeholder");
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(0L);
        user.setCrystals(0L);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private Course makeCourse(String technology) {
        Course course = new Course();
        course.setTitle("Core " + technology);
        course.setTechnology(technology);
        course.setPublished(false);
        course.setCreatedAt(Instant.now());
        return course;
    }

    private Lecture makeLecture() {
        Course course = courseRepository.save(makeCourse("Java"));
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle("Module");
        module.setOrderIndex(1);
        CourseModule savedModule = courseModuleRepository.save(module);

        Lecture lecture = new Lecture();
        lecture.setModule(savedModule);
        lecture.setTitle("Lecture 1");
        lecture.setOrderIndex(1);
        return lecture;
    }

    private Task makeTask() {
        Lecture lecture = lectureRepository.save(makeLecture());
        Task task = new Task();
        task.setLecture(lecture);
        task.setTitle("Hello World");
        task.setDifficulty("BEGINNER");
        task.setXpReward(10L);
        return task;
    }
}
