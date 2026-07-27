package com.javaacademy.platform.catalog.mapper;

import com.javaacademy.platform.catalog.dto.CourseDetailResponse;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.LectureResponse;
import com.javaacademy.platform.catalog.dto.LectureStubResponse;
import com.javaacademy.platform.catalog.dto.ModuleResponse;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.dto.TaskStubResponse;
import com.javaacademy.platform.catalog.entity.Course;
import com.javaacademy.platform.catalog.entity.CourseModule;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CatalogMapper {

    public CourseResponse toCourseResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getTechnology(),
                course.getCreatedAt());
    }

    public CourseDetailResponse toCourseDetailResponse(Course course, List<ModuleResponse> modules) {
        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getTechnology(),
                course.getCreatedAt(),
                modules);
    }

    public ModuleResponse toModuleResponse(CourseModule module, List<LectureStubResponse> lectureStubs) {
        return new ModuleResponse(module.getId(), module.getTitle(), module.getOrderIndex(), lectureStubs);
    }

    public LectureStubResponse toLectureStubResponse(Lecture lecture, List<TaskStubResponse> taskStubs) {
        return new LectureStubResponse(lecture.getId(), lecture.getTitle(), lecture.getOrderIndex(), taskStubs);
    }

    public LectureResponse toLectureResponse(Lecture lecture, List<TaskStubResponse> taskStubs) {
        return new LectureResponse(
                lecture.getId(),
                lecture.getModule().getCourse().getId(),
                lecture.getTitle(),
                lecture.getContentMarkdown(),
                lecture.getOrderIndex(),
                taskStubs);
    }

    public TaskStubResponse toTaskStubResponse(Task task) {
        return new TaskStubResponse(task.getId(), task.getTitle(), task.getDifficulty(), task.getXpReward());
    }

    public TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getLecture().getModule().getCourse().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDifficulty(),
                task.getTemplateCode(),
                task.getXpReward());
    }
}
