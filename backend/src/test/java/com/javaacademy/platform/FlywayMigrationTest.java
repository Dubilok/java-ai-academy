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
            "refresh_tokens",
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
            "ai_evaluation",
            "ai_hints");

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
            "idx_interview_answers_question_id",
            "idx_refresh_tokens_user_id",
            "idx_ai_hints_user_id",
            "idx_ai_hints_task_id");

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
    void allMigrations_areAppliedWithNoChecksumMismatch() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(7);
        assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(applied[0].getDescription()).isEqualTo("initial schema");
        assertThat(applied[0].getState().isApplied()).isTrue();
        assertThat(applied[0].getState().isFailed()).isFalse();
        assertThat(applied[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(applied[1].getDescription()).isEqualTo("refresh tokens");
        assertThat(applied[1].getState().isApplied()).isTrue();
        assertThat(applied[1].getState().isFailed()).isFalse();
        assertThat(applied[2].getVersion().getVersion()).isEqualTo("3");
        assertThat(applied[2].getDescription()).isEqualTo("status enum constraints");
        assertThat(applied[2].getState().isApplied()).isTrue();
        assertThat(applied[2].getState().isFailed()).isFalse();
        assertThat(applied[3].getVersion().getVersion()).isEqualTo("4");
        assertThat(applied[3].getDescription()).isEqualTo("seed data");
        assertThat(applied[3].getState().isApplied()).isTrue();
        assertThat(applied[3].getState().isFailed()).isFalse();
        assertThat(applied[4].getVersion().getVersion()).isEqualTo("5");
        assertThat(applied[4].getDescription()).isEqualTo("ai generation log enum constraints");
        assertThat(applied[4].getState().isApplied()).isTrue();
        assertThat(applied[4].getState().isFailed()).isFalse();
        assertThat(applied[5].getVersion().getVersion()).isEqualTo("6");
        assertThat(applied[5].getDescription()).isEqualTo("ai hints");
        assertThat(applied[5].getState().isApplied()).isTrue();
        assertThat(applied[5].getState().isFailed()).isFalse();
        assertThat(applied[6].getVersion().getVersion()).isEqualTo("7");
        assertThat(applied[6].getDescription()).isEqualTo("interview difficulty check");
        assertThat(applied[6].getState().isApplied()).isTrue();
        assertThat(applied[6].getState().isFailed()).isFalse();
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
