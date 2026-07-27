package com.javaacademy.platform.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Request to generate AI content. Two modes:
 *
 * <p><b>Curriculum mode</b> (curriculum != null): generates a full course from a confirmed learning
 * path — multiple modules, lectures, and tasks. The technology and curriculum fields are used.
 *
 * <p><b>Single-lecture mode</b> (curriculum == null): legacy mode that generates one
 * lecture+task.
 *
 * <ul>
 *   <li>courseId=null → create a brand-new course
 *   <li>courseId set, moduleId=null → add a new module to the existing course
 *   <li>courseId set, moduleId set → add a lecture+task to the existing module
 * </ul>
 */
public record GenerateCourseRequest(
        @NotBlank @Size(max = 100) String technology,
        @Nullable UUID courseId,
        @Nullable UUID moduleId,
        @Nullable @Size(max = 100) String moduleName,
        @Nullable @Valid ConfirmedCurriculum curriculum) {}
