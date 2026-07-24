package com.javaacademy.platform.progress.service;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.progress.dto.CourseProgressResponse;
import com.javaacademy.platform.progress.dto.MeResponse;
import com.javaacademy.platform.progress.dto.ProgressSummaryResponse;
import com.javaacademy.platform.progress.enums.ProgressStatus;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import com.javaacademy.platform.progress.util.XpCalculator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final UserProgressRepository userProgressRepository;

    public MeResponse getMe(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + email));
        int level = XpCalculator.calculateLevel(user.getXpPoints());
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getXpPoints(),
                user.getCrystals(),
                level,
                0, // streak: not tracked in V1 schema; always 0
                user.getCreatedAt());
    }

    public ProgressSummaryResponse getProgress(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + email));
        UUID userId = user.getId();

        List<Course> courses = courseRepository.findAllPublished(Pageable.unpaged());
        List<CourseProgressResponse> items = courses.stream()
                .map(course -> buildCourseProgress(userId, course))
                .toList();

        return new ProgressSummaryResponse(items);
    }

    private CourseProgressResponse buildCourseProgress(UUID userId, Course course) {
        long totalTasks = taskRepository.countTasksInCourse(course.getId());
        long passedTasks =
                userProgressRepository.countByUserIdAndCourseIdAndStatus(userId, course.getId(), ProgressStatus.PASSED);
        int completionPercent = totalTasks == 0 ? 0 : (int) (passedTasks * 100L / totalTasks);
        return new CourseProgressResponse(
                course.getId(), course.getTitle(), totalTasks, passedTasks, completionPercent);
    }
}
