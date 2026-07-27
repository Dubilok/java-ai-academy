package com.javaacademy.platform.interview.entity;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.interview.enums.InterviewMode;
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String technology;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMode mode = InterviewMode.TEXT;

    @Nullable
    @Column(name = "topic_map_json")
    private String topicMapJson;

    @Nullable
    @Column(name = "total_turns")
    private Integer totalTurns;

    @Nullable
    @Column(name = "max_turns")
    private Integer maxTurns;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_question_id")
    private InterviewQuestion currentQuestion;

    @Nullable
    @Column
    private Integer score;

    @Nullable
    @Column(name = "report_json")
    private String reportJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
