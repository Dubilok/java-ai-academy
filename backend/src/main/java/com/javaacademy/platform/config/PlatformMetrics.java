package com.javaacademy.platform.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Central home for custom application meters.
 * Inject this bean to record submissions, sandbox outcomes, and AI token spend.
 */
@Component
public class PlatformMetrics {

    private final Timer submissionDurationTimer;
    private final Counter sandboxFailureCounter;
    private final Counter tokenSpendCounter;

    public PlatformMetrics(MeterRegistry registry) {
        this.submissionDurationTimer = Timer.builder("academy.submission.duration")
                .description("End-to-end sandbox execution time per submission")
                .register(registry);

        this.sandboxFailureCounter = Counter.builder("academy.sandbox.failures")
                .description("Number of submissions that resulted in a sandbox FAILED verdict")
                .register(registry);

        this.tokenSpendCounter = Counter.builder("academy.ai.tokens")
                .description("Total LLM tokens consumed (prompt + completion)")
                .register(registry);
    }

    public Timer submissionDurationTimer() {
        return submissionDurationTimer;
    }

    public void recordSandboxFailure() {
        sandboxFailureCounter.increment();
    }

    public void recordTokens(int promptTokens, int completionTokens) {
        tokenSpendCounter.increment(promptTokens + completionTokens);
    }
}
