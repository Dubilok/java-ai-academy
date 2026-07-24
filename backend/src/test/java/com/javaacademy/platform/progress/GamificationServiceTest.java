package com.javaacademy.platform.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.progress.service.GamificationService;
import com.javaacademy.platform.progress.util.XpCalculator;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    TaskRepository taskRepository;

    @InjectMocks
    GamificationService service;

    static final UUID USER_ID = UUID.randomUUID();
    static final UUID TASK_ID = UUID.randomUUID();

    // ── awardTaskCompletion ───────────────────────────────────────────────────

    @Test
    void awardTaskCompletion_addsTaskXpRewardToUser() {
        User user = userWithXp(500L);
        Task task = taskWithXpReward(100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.getReferenceById(TASK_ID)).thenReturn(task);

        service.awardTaskCompletion(USER_ID, TASK_ID);

        assertThat(user.getXpPoints()).isEqualTo(600L);
    }

    @Test
    void awardTaskCompletion_incrementsCrystalsByOne() {
        User user = userWithXp(0L);
        user.setCrystals(3L);
        Task task = taskWithXpReward(50L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.getReferenceById(TASK_ID)).thenReturn(task);

        service.awardTaskCompletion(USER_ID, TASK_ID);

        assertThat(user.getCrystals()).isEqualTo(4L);
    }

    @Test
    void awardTaskCompletion_whenUserNotFound_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.awardTaskCompletion(USER_ID, TASK_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── XpCalculator.calculateLevel ───────────────────────────────────────────

    @Test
    void calculateLevel_atZeroXp_returnsOne() {
        assertThat(XpCalculator.calculateLevel(0L)).isEqualTo(1);
    }

    @Test
    void calculateLevel_atNegativeXp_returnsOne() {
        assertThat(XpCalculator.calculateLevel(-500L)).isEqualTo(1);
    }

    @Test
    void calculateLevel_at100Xp_returnsTwo() {
        assertThat(XpCalculator.calculateLevel(100L)).isEqualTo(2);
    }

    @Test
    void calculateLevel_at400Xp_returnsThree() {
        assertThat(XpCalculator.calculateLevel(400L)).isEqualTo(3);
    }

    @Test
    void calculateLevel_at900Xp_returnsFour() {
        assertThat(XpCalculator.calculateLevel(900L)).isEqualTo(4);
    }

    @Test
    void calculateLevel_justBelowNextThreshold_doesNotAdvance() {
        // 399 XP: sqrt(3.99) ≈ 1.997 → floor = 1 → level 2
        assertThat(XpCalculator.calculateLevel(399L)).isEqualTo(2);
    }

    @Test
    void calculateLevel_atVeryHighXp_capsAtFifty() {
        assertThat(XpCalculator.calculateLevel(500_000L)).isEqualTo(50);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithXp(long xpPoints) {
        User user = new User();
        user.setEmail("student@test.com");
        user.setPasswordHash("$2a$12$hash");
        user.setRole("ROLE_STUDENT");
        user.setXpPoints(xpPoints);
        user.setCrystals(0L);
        return user;
    }

    private Task taskWithXpReward(long xpReward) {
        Task task = new Task();
        task.setTitle("Test task");
        task.setDifficulty("EASY");
        task.setXpReward(xpReward);
        return task;
    }
}
