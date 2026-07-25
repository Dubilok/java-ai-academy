package com.javaacademy.platform.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.catalog.controller.CatalogController;
import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.IdeBootstrapCourseResponse;
import com.javaacademy.platform.catalog.dto.IdeBootstrapTaskResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.ModuleResponse;
import com.javaacademy.platform.catalog.dto.PagedResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.service.CatalogService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class CatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CatalogService catalogService;

    @MockBean
    JwtService jwtService;

    // ── GET /courses (public) ─────────────────────────────────────────────────

    @Test
    void listCourses_unauthenticated_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        CourseResponse course =
                new CourseResponse(id, "Java Basics", "Intro", "Java", Instant.parse("2026-01-01T00:00:00Z"));
        given(catalogService.listPublishedCourses(isNull(), anyInt()))
                .willReturn(new PagedResponse<>(List.of(course), null));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Java Basics"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void listCourses_withCursorAndLimit_passedToService() throws Exception {
        given(catalogService.listPublishedCourses(any(), anyInt()))
                .willReturn(new PagedResponse<>(List.of(), "next-cursor-token"));

        mockMvc.perform(get("/api/v1/courses").param("cursor", "some-cursor").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("next-cursor-token"));
    }

    // ── GET /courses/{id} ─────────────────────────────────────────────────────

    @Test
    void getCourse_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getCourse_authenticated_returns200WithModuleTree() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseDetailResponse detail = new CourseDetailResponse(
                courseId, "Spring Boot", "Description", "Spring", Instant.parse("2026-01-01T00:00:00Z"), List.of());
        given(catalogService.getCourse(courseId)).willReturn(detail);

        mockMvc.perform(get("/api/v1/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId.toString()))
                .andExpect(jsonPath("$.title").value("Spring Boot"))
                .andExpect(jsonPath("$.modules").isArray());
    }

    @Test
    @WithMockUser
    void getCourse_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(catalogService.getCourse(id))
                .willThrow(new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + id));

        mockMvc.perform(get("/api/v1/courses/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getCourse_withModules_returnsModuleAndLectureTree() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ModuleResponse module = new ModuleResponse(moduleId, "Module 1", 1, List.of());
        CourseDetailResponse detail = new CourseDetailResponse(
                courseId, "Spring Boot", null, "Spring", Instant.parse("2026-01-01T00:00:00Z"), List.of(module));
        given(catalogService.getCourse(courseId)).willReturn(detail);

        mockMvc.perform(get("/api/v1/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].id").value(moduleId.toString()))
                .andExpect(jsonPath("$.modules[0].orderIndex").value(1));
    }

    // ── GET /lectures/{id} ────────────────────────────────────────────────────

    @Test
    void getLecture_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/lectures/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getLecture_authenticated_returns200WithTaskStubs() throws Exception {
        UUID lectureId = UUID.randomUUID();
        LectureResponse lecture = new LectureResponse(lectureId, "JUnit 5", "# Content", 1, List.of());
        given(catalogService.getLecture(lectureId)).willReturn(lecture);

        mockMvc.perform(get("/api/v1/lectures/" + lectureId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lectureId.toString()))
                .andExpect(jsonPath("$.title").value("JUnit 5"))
                .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    @WithMockUser
    void getLecture_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(catalogService.getLecture(id)).willThrow(new ApiException(HttpStatus.NOT_FOUND, "Lecture not found"));

        mockMvc.perform(get("/api/v1/lectures/" + id)).andExpect(status().isNotFound());
    }

    // ── GET /tasks/{id} ───────────────────────────────────────────────────────

    @Test
    void getTask_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getTask_authenticated_returns200AndNeverExposesTestCode() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse task = new TaskResponse(taskId, "Hello World", "Print hello", "EASY", "// template", 50L);
        given(catalogService.getTask(taskId)).willReturn(task);

        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Hello World"))
                .andExpect(jsonPath("$.templateCode").value("// template"))
                .andExpect(jsonPath("$.testCode").doesNotExist())
                .andExpect(jsonPath("$.solutionCode").doesNotExist());
    }

    @Test
    @WithMockUser
    void getTask_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(catalogService.getTask(id)).willThrow(new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        mockMvc.perform(get("/api/v1/tasks/" + id)).andExpect(status().isNotFound());
    }

    // ── GET /ide/bootstrap ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void ideBootstrap_authenticated_returnsCourseList() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        IdeBootstrapTaskResponse task = new IdeBootstrapTaskResponse(taskId, "Hello World", "EASY", 10L);
        IdeBootstrapCourseResponse course = new IdeBootstrapCourseResponse(courseId, "Java 21", "Java", List.of(task));
        given(catalogService.getIdeBootstrap()).willReturn(List.of(course));

        mockMvc.perform(get("/api/v1/ide/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(courseId.toString()))
                .andExpect(jsonPath("$[0].title").value("Java 21"))
                .andExpect(jsonPath("$[0].technology").value("Java"))
                .andExpect(jsonPath("$[0].tasks[0].title").value("Hello World"))
                .andExpect(jsonPath("$[0].tasks[0].xpReward").value(10));
    }

    @Test
    void ideBootstrap_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ide/bootstrap")).andExpect(status().isUnauthorized());
    }
}
