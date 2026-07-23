package com.javaacademy.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers
class FlywayMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "courses",
            "modules",
            "lectures",
            "tasks",
            "user_progress",
            "submissions",
            "interview_questions",
            "interview_sessions",
            "interview_answers",
            "ai_generation_log",
            "ai_evaluation");

    private static final List<String> EXPECTED_INDEXES = List.of(
            "idx_courses_technology",
            "idx_modules_course_id",
            "idx_lectures_module_id",
            "idx_tasks_lecture_id",
            "idx_user_progress_user_id",
            "idx_user_progress_task_id",
            "idx_submissions_user_id",
            "idx_submissions_task_id",
            "idx_submissions_status",
            "idx_interview_questions_technology",
            "idx_interview_questions_category",
            "idx_interview_questions_difficulty",
            "idx_interview_sessions_user_id",
            "idx_interview_answers_session_id",
            "idx_interview_answers_question_id");

    @Test
    void allExpectedTables_existAfterMigration() {
        EXPECTED_TABLES.forEach(table -> assertThat(tableExists(table))
                .as("table '%s' should exist after migration", table)
                .isTrue());
    }

    @Test
    void allExpectedIndexes_existAfterMigration() {
        EXPECTED_INDEXES.forEach(index -> assertThat(indexExists(index))
                .as("index '%s' should exist after migration", index)
                .isTrue());
    }

    @Test
    void migration_v1_isAppliedAndHasNoChecksum_mismatch() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(1);
        assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(applied[0].getDescription()).isEqualTo("initial schema");
        assertThat(applied[0].getState().isApplied()).isTrue();
        assertThat(applied[0].getState().isFailed()).isFalse();
    }

    @Test
    void migration_noPendingMigrations_afterStartup() {
        assertThat(flyway.info().pending()).isEmpty();
    }

    private boolean tableExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables" + " WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                name);
        return count != null && count == 1;
    }

    private boolean indexExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?", Integer.class, name);
        return count != null && count == 1;
    }
}
