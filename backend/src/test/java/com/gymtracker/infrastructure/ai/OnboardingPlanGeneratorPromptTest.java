package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ===== T070-BUG-006-ENUM: Prompt alignment tests =====

/**
 * Verifies that {@link OnboardingPlanGenerator#buildPrompt} references only the valid
 * {@link com.gymtracker.domain.ExerciseType} enum constants ({@code STRENGTH},
 * {@code BODYWEIGHT}, {@code CARDIO}) and does NOT contain the obsolete {@code FLEXIBILITY}
 * value that caused the production crash.
 */
@SpringBootTest(properties = {
        "azure.openai.endpoint=https://prompt-test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-gpt-deployment"
})
class OnboardingPlanGeneratorPromptTest {

    /** Not needed for prompt tests, but required to satisfy the Spring context. */
    @MockitoBean
    private LangChainSessionProcessor langChainSessionProcessor;

    @Autowired
    private OnboardingPlanGenerator generator;

    private static final OnboardingSubmissionRequest SAMPLE_REQUEST =
            new OnboardingSubmissionRequest(28, BigDecimal.valueOf(70), WeightUnit.KG,
                    OnboardingPrimaryGoal.LOSE_WEIGHT, null);

    @Test
    void buildPrompt_ContainsStrengthType() {
        String prompt = generator.buildPrompt(SAMPLE_REQUEST);
        assertThat(prompt).contains("STRENGTH");
    }

    @Test
    void buildPrompt_ContainsBodyweightType() {
        String prompt = generator.buildPrompt(SAMPLE_REQUEST);
        assertThat(prompt).contains("BODYWEIGHT");
    }

    @Test
    void buildPrompt_ContainsCardioType() {
        String prompt = generator.buildPrompt(SAMPLE_REQUEST);
        assertThat(prompt).contains("CARDIO");
    }

    @Test
    void buildPrompt_DoesNotContainFlexibilityType() {
        String prompt = generator.buildPrompt(SAMPLE_REQUEST);
        assertThat(prompt).doesNotContain("FLEXIBILITY");
    }

    @Test
    void buildPrompt_DoesNotReferenceFlexibilityWorkPhrase() {
        String prompt = generator.buildPrompt(SAMPLE_REQUEST);
        assertThat(prompt).doesNotContainIgnoringCase("flexibility work");
    }
}


