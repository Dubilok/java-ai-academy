package com.javaacademy.platform.progress.dto;

import java.util.UUID;

public record CourseProgressResponse(
        UUID courseId, String courseTitle, long totalTasks, long passedTasks, int completionPercent) {}
