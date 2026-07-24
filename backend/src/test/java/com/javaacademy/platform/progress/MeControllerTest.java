package com.javaacademy.platform.progress;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.config.SecurityConfig;
import com.javaacademy.platform.progress.controller.MeController;
import com.javaacademy.platform.progress.dto.CourseProgressResponse;
import com.javaacademy.platform.progress.dto.MeResponse;
import com.javaacademy.platform.progress.dto.ProgressSummaryResponse;
import com.javaacademy.platform.progress.service.MeService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MeService meService;

    @MockBean
    JwtService jwtService;

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "student@test.com")
    void getMe_authenticated_returns200WithProfile() throws Exception {
        MeResponse response = new MeResponse(
                UUID.randomUUID(),
                "student@test.com",
                "ROLE_STUDENT",
                400L,
                2L,
                3,
                0,
                Instant.parse("2026-07-24T10:00:00Z"));
        given(meService.getMe(eq("student@test.com"))).willReturn(response);

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.xpPoints").value(400))
                .andExpect(jsonPath("$.level").value(3))
                .andExpect(jsonPath("$.streak").value(0));
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    // ── GET /me/progress ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "student@test.com")
    void getProgress_authenticated_returns200WithItems() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseProgressResponse item = new CourseProgressResponse(courseId, "Java Basics", 10L, 3L, 30);
        given(meService.getProgress(eq("student@test.com"))).willReturn(new ProgressSummaryResponse(List.of(item)));

        mockMvc.perform(get("/api/v1/me/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].courseTitle").value("Java Basics"))
                .andExpect(jsonPath("$.items[0].totalTasks").value(10))
                .andExpect(jsonPath("$.items[0].passedTasks").value(3))
                .andExpect(jsonPath("$.items[0].completionPercent").value(30));
    }

    @Test
    void getProgress_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/progress")).andExpect(status().isUnauthorized());
    }
}
