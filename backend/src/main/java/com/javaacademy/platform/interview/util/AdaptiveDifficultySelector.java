package com.javaacademy.platform.interview.util;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Selects the target difficulty for the next interview question based on the student's
 * rolling performance over recent answers.
 *
 * <p>Algorithm (rolling window of last {@value #ROLLING_WINDOW} scored answers):
 * <ul>
 *   <li>Average score ≥ 70 → step up difficulty (cap at EXPERT)
 *   <li>Average score ≤ 30 → step down difficulty (floor at BEGINNER)
 *   <li>Otherwise → keep current difficulty
 * </ul>
 *
 * <p>When fewer than {@value #MIN_SCORED_ANSWERS} answers have a non-null score, the
 * current difficulty is returned unchanged (warm-up phase).
 */
@UtilityClass
public class AdaptiveDifficultySelector {

    static final int ROLLING_WINDOW = 3;
    static final int MIN_SCORED_ANSWERS = 2;
    static final int STEP_UP_THRESHOLD = 70;
    static final int STEP_DOWN_THRESHOLD = 30;

    private static final List<InterviewDifficulty> ORDERED = List.of(
            InterviewDifficulty.BEGINNER,
            InterviewDifficulty.INTERMEDIATE,
            InterviewDifficulty.ADVANCED,
            InterviewDifficulty.EXPERT);

    /**
     * Returns the recommended difficulty for the next question.
     *
     * @param currentDifficulty the difficulty of the question just answered
     * @param recentScores      scores of the last N answers (oldest first); nulls are ignored
     * @return target difficulty for the next question
     */
    public static InterviewDifficulty selectNext(InterviewDifficulty currentDifficulty, List<Integer> recentScores) {
        List<Integer> scored =
                recentScores.stream().filter(score -> score != null).toList();

        if (scored.size() < MIN_SCORED_ANSWERS) {
            return currentDifficulty;
        }

        List<Integer> window =
                scored.size() > ROLLING_WINDOW ? scored.subList(scored.size() - ROLLING_WINDOW, scored.size()) : scored;

        double average = window.stream().mapToInt(Integer::intValue).average().orElse(50.0);

        if (average >= STEP_UP_THRESHOLD) {
            return stepUp(currentDifficulty);
        }
        if (average <= STEP_DOWN_THRESHOLD) {
            return stepDown(currentDifficulty);
        }
        return currentDifficulty;
    }

    private static InterviewDifficulty stepUp(InterviewDifficulty difficulty) {
        int index = ORDERED.indexOf(difficulty);
        return index < ORDERED.size() - 1 ? ORDERED.get(index + 1) : difficulty;
    }

    private static InterviewDifficulty stepDown(InterviewDifficulty difficulty) {
        int index = ORDERED.indexOf(difficulty);
        return index > 0 ? ORDERED.get(index - 1) : difficulty;
    }
}
