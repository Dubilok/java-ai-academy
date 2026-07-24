package com.javaacademy.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
class SeedDataTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seedCourse_isPublishedAndCorrectlyTitled() {
        String title = jdbc.queryForObject("SELECT title FROM courses LIMIT 1", String.class);
        assertThat(title).isEqualTo("Java 21 Fundamentals");

        Integer published =
                jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE is_published = TRUE", Integer.class);
        assertThat(published).isEqualTo(1);
    }

    @Test
    void seedModuleCount_isOne() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM modules", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void seedLectureCount_isThree() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM lectures", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void seedLectures_areOrderedCorrectly() {
        List<String> titles = jdbc.queryForList(
                "SELECT l.title FROM lectures l " + "JOIN modules m ON m.id = l.module_id " + "ORDER BY l.order_index",
                String.class);
        assertThat(titles).containsExactly("Working with Strings", "Control Flow", "Writing Methods");
    }

    @Test
    void seedTaskCount_isFive() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void seedTasks_allHaveTestCode() {
        Integer missingTest = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE test_code IS NULL OR test_code = ''", Integer.class);
        assertThat(missingTest).isEqualTo(0);
    }

    @Test
    void seedTasks_allHaveTemplateCode() {
        Integer missingTemplate = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE template_code IS NULL OR template_code = ''", Integer.class);
        assertThat(missingTemplate).isEqualTo(0);
    }

    @Test
    void seedTasks_allHaveSolutionCode() {
        Integer missingSolution = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE solution_code IS NULL OR solution_code = ''", Integer.class);
        assertThat(missingSolution).isEqualTo(0);
    }

    @Test
    void seedTasks_twoUnderStringsLecture_threeUnderOthers() {
        Integer stringsTaskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks t "
                        + "JOIN lectures l ON l.id = t.lecture_id "
                        + "WHERE l.title = 'Working with Strings'",
                Integer.class);
        assertThat(stringsTaskCount).isEqualTo(2);

        Integer controlFlowTaskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks t "
                        + "JOIN lectures l ON l.id = t.lecture_id "
                        + "WHERE l.title = 'Control Flow'",
                Integer.class);
        assertThat(controlFlowTaskCount).isEqualTo(2);

        Integer methodsTaskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks t "
                        + "JOIN lectures l ON l.id = t.lecture_id "
                        + "WHERE l.title = 'Writing Methods'",
                Integer.class);
        assertThat(methodsTaskCount).isEqualTo(1);
    }
}
