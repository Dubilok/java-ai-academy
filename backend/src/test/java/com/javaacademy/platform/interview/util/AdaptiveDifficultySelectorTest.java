package com.javaacademy.platform.interview.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdaptiveDifficultySelectorTest {

    // ── warm-up phase: fewer than MIN_SCORED_ANSWERS non-null scores ─────────

    @Test
    void selectNext_emptyScores_returnsCurrentDifficulty() {
        InterviewDifficulty result = AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of());
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_oneNullScore_returnsCurrentDifficulty() {
        List<Integer> scores = Arrays.asList((Integer) null);
        InterviewDifficulty result = AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, scores);
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_oneNonNullScore_returnsCurrentDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(90));
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    // ── step up ────────────────────────────────────────────────────────────────

    @Test
    void selectNext_averageAboveThreshold_stepsUpDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.BEGINNER, List.of(80, 85));
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_averageExactlyAtStepUpThreshold_stepsUp() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(70, 70));
        assertThat(result).isEqualTo(InterviewDifficulty.ADVANCED);
    }

    @Test
    void selectNext_stepUpFromIntermediate_returnsAdvanced() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(90, 95, 100));
        assertThat(result).isEqualTo(InterviewDifficulty.ADVANCED);
    }

    @Test
    void selectNext_stepUpFromAdvanced_returnsExpert() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.ADVANCED, List.of(75, 80, 90));
        assertThat(result).isEqualTo(InterviewDifficulty.EXPERT);
    }

    @Test
    void selectNext_alreadyAtExpert_stepsUpCapsAtExpert() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.EXPERT, List.of(85, 90, 95));
        assertThat(result).isEqualTo(InterviewDifficulty.EXPERT);
    }

    // ── step down ─────────────────────────────────────────────────────────────

    @Test
    void selectNext_averageBelowThreshold_stepsDownDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.ADVANCED, List.of(20, 25));
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_averageExactlyAtStepDownThreshold_stepsDown() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(30, 30));
        assertThat(result).isEqualTo(InterviewDifficulty.BEGINNER);
    }

    @Test
    void selectNext_stepDownFromExpert_returnsAdvanced() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.EXPERT, List.of(10, 15, 20));
        assertThat(result).isEqualTo(InterviewDifficulty.ADVANCED);
    }

    @Test
    void selectNext_alreadyAtBeginner_stepsDownCapsAtBeginner() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.BEGINNER, List.of(10, 20));
        assertThat(result).isEqualTo(InterviewDifficulty.BEGINNER);
    }

    // ── stay same ─────────────────────────────────────────────────────────────

    @Test
    void selectNext_averageInMiddleRange_retainsDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(40, 60));
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_averageJustAboveStepDownThreshold_retainsDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.ADVANCED, List.of(31, 50));
        assertThat(result).isEqualTo(InterviewDifficulty.ADVANCED);
    }

    @Test
    void selectNext_averageJustBelowStepUpThreshold_retainsDifficulty() {
        InterviewDifficulty result =
                AdaptiveDifficultySelector.selectNext(InterviewDifficulty.INTERMEDIATE, List.of(69, 69));
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    // ── rolling window uses only last ROLLING_WINDOW scores ──────────────────

    @Test
    void selectNext_manyScores_onlyLastWindowCountsForStepUp() {
        // First 10 scores are low, last 3 are high — should still step up
        List<Integer> scores = List.of(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 80, 85, 90);
        InterviewDifficulty result = AdaptiveDifficultySelector.selectNext(InterviewDifficulty.BEGINNER, scores);
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void selectNext_manyScores_onlyLastWindowCountsForStepDown() {
        // First 10 scores are high, last 3 are low — should still step down
        List<Integer> scores = List.of(90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 10, 15, 20);
        InterviewDifficulty result = AdaptiveDifficultySelector.selectNext(InterviewDifficulty.ADVANCED, scores);
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    // ── null scores mixed with real ones ─────────────────────────────────────

    @Test
    void selectNext_mixedNullAndRealScores_nullsIgnoredInAverage() {
        // [null, 80, null, 85] — 2 non-null >= MIN, average=82.5 >= 70 → step up
        List<Integer> scores = Arrays.asList(null, 80, null, 85);
        InterviewDifficulty result = AdaptiveDifficultySelector.selectNext(InterviewDifficulty.BEGINNER, scores);
        assertThat(result).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }
}
