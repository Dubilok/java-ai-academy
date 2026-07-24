package com.javaacademy.platform.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false)
    private String title;

    @Nullable
    @Column
    private String description;

    @Column(nullable = false)
    private String difficulty;

    @Nullable
    @Column(name = "template_code")
    private String templateCode;

    /** Never exposed in student-facing DTOs — enforced by ArchUnit test in E1-T8. */
    @Nullable
    @Column(name = "test_code")
    private String testCode;

    /** Never exposed in student-facing DTOs — enforced by ArchUnit test in E1-T8. */
    @Nullable
    @Column(name = "solution_code")
    private String solutionCode;

    @Column(name = "xp_reward", nullable = false)
    private long xpReward;
}
