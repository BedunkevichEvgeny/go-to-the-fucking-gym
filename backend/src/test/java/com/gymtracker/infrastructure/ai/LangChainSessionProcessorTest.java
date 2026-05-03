package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LangChainSessionProcessorTest {

    @Test
    void processRetriesTransientFailuresAndEventuallySucceeds() {
        AtomicInteger attempts = new AtomicInteger();

        LangChainSessionProcessor processor = new LangChainSessionProcessor(
                "https://integration.test.azure.openai",
                "test-key",
                "test-deployment",
                30,
                3
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                int attempt = attempts.incrementAndGet();
                if (attempt < 3) {
                    throw new IllegalStateException("503 service unavailable");
                }
                return "ok";
            }
        };

        String response = processor.process(sampleSummary());

        assertThat(response).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    // ── T066-BUG-004-TEST: fail-fast malformed/empty model-output ─────────────

    @Test
    void processThrowsOnEmptyModelOutput() {
        LangChainSessionProcessor processor = processorReturning("");

        assertThatThrownBy(() -> processor.process(sampleSummary()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty or blank");
    }

    @Test
    void processThrowsOnBlankModelOutput() {
        LangChainSessionProcessor processor = processorReturning("   \t\n  ");

        assertThatThrownBy(() -> processor.process(sampleSummary()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty or blank");
    }

    @Test
    void processThrowsOnNullModelOutput() {
        LangChainSessionProcessor processor = processorReturning(null);

        assertThatThrownBy(() -> processor.process(sampleSummary()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty or blank");
    }

    @Test
    void processDoesNotReturnHardcodedFallbackWhenModelFails() {
        LangChainSessionProcessor processor = new LangChainSessionProcessor(
                "https://integration.test.azure.openai",
                "test-key",
                "test-deployment",
                30,
                1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                throw new RuntimeException("upstream model failure");
            }
        };

        // Must propagate the upstream error — never swallow it and return any fallback string
        assertThatThrownBy(() -> processor.process(sampleSummary()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("upstream model failure");
    }

    @Test
    void processDoesNotReturnHardcodedFallbackOnMalformedResponse() {
        // A realistic "malformed" response: looks like JSON but truncated
        String malformedJson = "{\"proposal\": null, \"sessions\": [";

        LangChainSessionProcessor processor = processorReturning(malformedJson);

        // The processor must surface the raw (non-blank) response as-is for now;
        // crucially it must NOT silently replace it with fabricated proposal/session data.
        String result = processor.process(sampleSummary());

        assertThat(result).isEqualTo(malformedJson);
        assertThat(result).doesNotContain("Bench Press"); // no hardcoded workout data
        assertThat(result).doesNotContain("fallback");    // no silent fallback label
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private LangChainSessionProcessor processorReturning(String fixedResponse) {
        return new LangChainSessionProcessor(
                "https://integration.test.azure.openai",
                "test-key",
                "test-deployment",
                30,
                1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                return fixedResponse;
            }
        };
    }

    @Test
    void processDoesNotRetryNonTransientFailures() {
        AtomicInteger attempts = new AtomicInteger();

        LangChainSessionProcessor processor = new LangChainSessionProcessor(
                "https://integration.test.azure.openai",
                "test-key",
                "test-deployment",
                30,
                3
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                attempts.incrementAndGet();
                throw new IllegalStateException("invalid request");
            }
        };

        assertThatThrownBy(() -> processor.process(sampleSummary()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid request");
        assertThat(attempts.get()).isEqualTo(1);
    }

    private SessionSummaryDTO sampleSummary() {
        return new SessionSummaryDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.randomUUID(),
                SessionType.PROGRAM,
                LocalDate.of(2026, 4, 27),
                1800,
                new SessionSummaryDTO.FeelingsSummary(8, "Solid session"),
                new SessionSummaryDTO.UserPreferences("KG"),
                List.of(new SessionSummaryDTO.ExerciseSummary(
                        "Bench Press",
                        ExerciseType.STRENGTH,
                        new SessionSummaryDTO.ActualPerformance(3, 24, new BigDecimal("80"), null, null),
                        new SessionSummaryDTO.TargetPerformance(3, 8, new BigDecimal("82.5"), null, null))));
    }
}

