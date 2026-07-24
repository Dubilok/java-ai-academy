package com.javaacademy.platform.catalog.controller;

import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.PagedResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.service.CatalogService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/courses")
    public PagedResponse<CourseResponse> listCourses(
            @RequestParam(required = false) @Nullable String cursor, @RequestParam(defaultValue = "20") int limit) {
        return catalogService.listPublishedCourses(cursor, limit);
    }

    @GetMapping("/courses/{id}")
    public CourseDetailResponse getCourse(@PathVariable UUID id) {
        return catalogService.getCourse(id);
    }

    @GetMapping("/lectures/{id}")
    public LectureResponse getLecture(@PathVariable UUID id) {
        return catalogService.getLecture(id);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse getTask(@PathVariable UUID id) {
        return catalogService.getTask(id);
    }
}
