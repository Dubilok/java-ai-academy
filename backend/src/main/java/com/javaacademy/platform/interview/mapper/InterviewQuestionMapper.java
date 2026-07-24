package com.javaacademy.platform.interview.mapper;

import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InterviewQuestionMapper {

    public static InterviewQuestionResponse toResponse(InterviewQuestion question) {
        return new InterviewQuestionResponse(
                question.getId(),
                question.getTechnology(),
                question.getCategory(),
                question.getQuestion(),
                question.getShortAnswer(),
                question.getDetailedExplanation(),
                question.getDifficulty());
    }
}
