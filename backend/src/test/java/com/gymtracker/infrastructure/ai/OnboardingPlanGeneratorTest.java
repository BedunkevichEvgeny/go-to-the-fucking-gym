package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.dto.ExerciseDto;
import com.gymtracker.infrastructure.ai.dto.OnboardingPlanDto;
import com.gymtracker.infrastructure.ai.dto.SessionDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ===== T068-BUG-006-TEST: Regression tests for FLEXIBILITY / unknown ExerciseType crash =====

/**
 * Regression tests verifying that {@link OnboardingPlanGenerator} does NOT throw
 * {@link IllegalArgumentException} for unknown {@link ExerciseType} values that the LLM
 * may return (e.g., {@code "FLEXIBILITY"}) and that valid types like {@code BODYWEIGHT}
 * are mapped correctly.
 *
 * <p>{@link LangChainSessionProcessor} is replaced with a {@link MockitoBean} so
 * {@code processOnboarding()} returns a controlled {@link OnboardingPlanDto} directly,
 * without any network calls or LangChain4j deserialization.
 */
@SpringBootTest(properties = {
        "azure.openai.endpoint=https://regression.test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-gpt-deployment"
})
class OnboardingPlanGeneratorTest {

    @MockitoBean
    private LangChainSessionProcessor langChainSessionProcessor;

    @Autowired
    private OnboardingPlanGenerator onboardingPlanGenerator;

    private static final OnboardingSubmissionRequest TYPICAL_REQUEST =
            new OnboardingSubmissionRequest(30, BigDecimal.valueOf(75), WeightUnit.KG,
                    OnboardingPrimaryGoal.STRENGTH, null);

    // ── BUG-006 regression: FLEXIBILITY type must NOT crash the generator ────

    /**
     * Prior to the fix, {@code ExerciseType.valueOf("FLEXIBILITY")} threw
     * {@link IllegalArgumentException}.  After the fix the generator uses structured output
     * so the processor returns a typed DTO; the {@code SafeExerciseTypeDeserializer} falls
     * back to {@code STRENGTH}.  Here we simulate a plan where the type has already been
     * resolved to {@code STRENGTH} (the fallback), verifying the generator does not throw.
     */
    @Test
    void generateInitialProposal_DoesNotThrowForFlexibilityFallbackType() {
        // SafeExerciseTypeDeserializer maps "FLEXIBILITY" → STRENGTH; we simulate that here
        OnboardingPlanDto planWithFallback = planOf(
                exerciseDto("Yoga Flow", ExerciseType.STRENGTH)); // FLEXIBILITY → STRENGTH fallback

        when(langChainSessionProcessor.processOnboarding(anyString(), anyString()))
                .thenReturn(planWithFallback);

        assertThatCode(() -> onboardingPlanGenerator.generateInitialProposal(UUID.randomUUID(), TYPICAL_REQUEST))
                .doesNotThrowAnyException();
    }

    // ── BODYWEIGHT type maps correctly ────────────────────────────────────────

    @Test
    void generateInitialProposal_BodyweightTypeIsMappedCorrectly() {
        OnboardingPlanDto planWithBodyweight = planOf(
                exerciseDto("Pull-Up", ExerciseType.BODYWEIGHT),
                exerciseDto("Dip", ExerciseType.BODYWEIGHT));

        when(langChainSessionProcessor.processOnboarding(anyString(), anyString()))
                .thenReturn(planWithBodyweight);

        PlanProposalResponse response =
                onboardingPlanGenerator.generateInitialProposal(UUID.randomUUID(), TYPICAL_REQUEST);

        var types = response.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseType())
                .toList();
        assertThat(types).containsOnly(ExerciseType.BODYWEIGHT);
    }

    // ── null / unknown type falls back to STRENGTH and does not crash ────────

    /**
     * When {@code ExerciseDto.type()} is {@code null} (e.g., unknown enum deserialization
     * resulted in null rather than a fallback), the generator must default to {@code STRENGTH}
     * rather than propagating a {@code NullPointerException}.
     */
    @Test
    void generateInitialProposal_NullExerciseTypeDefaultsToStrength() {
        OnboardingPlanDto planWithNullType = planOf(
                new ExerciseDto("Resistance Band Row", null, 3, 12, null, WeightUnit.KG, null));

        when(langChainSessionProcessor.processOnboarding(anyString(), anyString()))
                .thenReturn(planWithNullType);

        PlanProposalResponse response =
                onboardingPlanGenerator.generateInitialProposal(UUID.randomUUID(), TYPICAL_REQUEST);

        var types = response.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseType())
                .toList();
        assertThat(types).containsOnly(ExerciseType.STRENGTH);
    }

    // ── session list is non-empty after parsing a response with mixed types ──

    @Test
    void generateInitialProposal_SessionListIsNonEmptyWithMixedTypes() {
        OnboardingPlanDto mixedPlan = new OnboardingPlanDto(List.of(
                new SessionDto(1, "Full Body A", List.of(
                        exerciseDto("Squat", ExerciseType.STRENGTH),
                        exerciseDto("Burpee", ExerciseType.BODYWEIGHT),
                        exerciseDto("Rowing", ExerciseType.CARDIO))),
                new SessionDto(2, "Full Body B", List.of(
                        exerciseDto("Deadlift", ExerciseType.STRENGTH),
                        new ExerciseDto("Unknown Move", null, 2, 10, null, WeightUnit.KG, null)))));

        when(langChainSessionProcessor.processOnboarding(anyString(), anyString()))
                .thenReturn(mixedPlan);

        PlanProposalResponse response =
                onboardingPlanGenerator.generateInitialProposal(UUID.randomUUID(), TYPICAL_REQUEST);

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions()).allMatch(s -> !s.exercises().isEmpty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static OnboardingPlanDto planOf(ExerciseDto... exercises) {
        return new OnboardingPlanDto(List.of(
                new SessionDto(1, "Test Session", List.of(exercises))));
    }

    private static ExerciseDto exerciseDto(String name, ExerciseType type) {
        return new ExerciseDto(name, type, 3, 10, null, WeightUnit.KG, null);
    }
}

