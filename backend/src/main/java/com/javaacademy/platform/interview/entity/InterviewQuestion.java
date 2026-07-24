package com.javaacademy.platform.interview.entity;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String technology;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String question;

    @Nullable
    @Column(name = "short_answer")
    private String shortAnswer;

    @Nullable
    @Column(name = "detailed_explanation")
    private String detailedExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewDifficulty difficulty;
}
