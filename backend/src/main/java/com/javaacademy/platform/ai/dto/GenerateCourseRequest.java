package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Request to generate AI content.
 *
 * <ul>
 *   <li>courseId=null → create a brand-new course</li>
 *   <li>courseId set, moduleId=null → add a new module to the existing course</li>
 *   <li>courseId set, moduleId set → add a lecture+task to the existing module</li>
 * </ul>
 */
public record GenerateCourseRequest(
        @NotBlank @Size(max = 100) String technology,
        @Nullable UUID courseId,
        @Nullable UUID moduleId,
        @Nullable @Size(max = 100) String moduleName) {}
