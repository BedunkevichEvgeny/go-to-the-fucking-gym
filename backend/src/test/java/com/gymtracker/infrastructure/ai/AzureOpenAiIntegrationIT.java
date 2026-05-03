package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.dto.ExerciseDto;
import com.gymtracker.infrastructure.ai.dto.OnboardingPlanDto;
import com.gymtracker.infrastructure.ai.dto.SessionDto;
import dev.langchain4j.model.chat.ChatModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

// ===== T065-BUG-003-TEST: LangChain/Azure Integration Contract Tests =====

/**
 * Integration tests verifying that {@link LangChainSessionProcessor} (backed by
 * {@code AiServices}) routes through the real {@link ChatModel} bean and that the
 * downstream {@link OnboardingPlanGenerator} correctly consumes the model's structured output.
 *
 * <p>The {@link ChatModel} is replaced with a {@link MockitoBean} so no actual Azure
 * OpenAI network calls are made, yet the full LangChain4j {@code AiServices} proxy
 * pipeline is exercised.
 */
@SpringBootTest(properties = {
        "azure.openai.endpoint=https://integration.test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-gpt-deployment"
})
class AzureOpenAiIntegrationIT {

    /** Mock the underlying chat model — the AiServices proxy delegates to this. */
    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private LangChainSessionProcessor processor;

    @Autowired
    private OnboardingPlanGenerator onboardingPlanGenerator;

    private static final String STUB_ONBOARDING_JSON = """
            {"sessions": [
              {"sequenceNumber": 1, "name": "Strength Foundation",
               "exercises": [
                 {"name": "Goblet Squat", "type": "STRENGTH",
                  "targetSets": 3, "targetReps": 12,
                  "targetWeight": 16.0, "weightUnit": "KG", "durationSeconds": null},
                 {"name": "Push-Up", "type": "STRENGTH",
                  "targetSets": 3, "targetReps": 15,
                  "targetWeight": null, "weightUnit": "KG", "durationSeconds": null}
               ]}
            ]}
            """;

    @BeforeEach
    void stubChatModel() {
        // Default stub: return valid onboarding JSON for all tests.
        // Tests that need different behaviour re-stub within the test method.
        when(chatModel.chat(anyString())).thenReturn(STUB_ONBOARDING_JSON);
    }

    // ── contract: AiServices proxy delegates to ChatModel bean ───────────────

    @Test
    void processor_DelegatesCallsToChatModelBean() {
        when(chatModel.chat(anyString())).thenReturn(STUB_ONBOARDING_JSON);

        String result = processor.process("mem-1", "test prompt");

        assertThat(result).isEqualTo(STUB_ONBOARDING_JSON.trim());
    }

    @Test
    void processor_ReturnsModelOutputVerbatim() {
        String expected = "Session analysis: Good progression. Increase weight by 2.5 kg.";
        when(chatModel.chat(anyString())).thenReturn(expected);

        String result = processor.process("mem-2", "analyse my session");

        assertThat(result).isEqualTo(expected);
    }

    // ── contract: output does NOT contain synthetic stub signature ─────────

    @Test
    void contractTest_ProcessorOutputDoesNotContainSyntheticStubSignature() {
        String naturalResponse =
                "Session analysis: Good progression observed. Recommend increasing weight by 2.5kg next session.";
        when(chatModel.chat(anyString())).thenReturn(naturalResponse);

        String result = processor.process("mem-3", "analyse session");

        assertThat(result).doesNotContain("azure-openai[endpoint=");
        assertThat(result).doesNotContain("accepted prompt hash=");
        assertThat(result).doesNotContain("apiKey=present");
        assertThat(result).contains("progression");
    }

    // ── contract: valid structured output is consumable by onboarding generator ──

    @Test
    void contractTest_ValidJsonOutputIsConsumableByOnboardingParser() {
        when(chatModel.chat(anyString())).thenReturn(STUB_ONBOARDING_JSON);

        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                32, BigDecimal.valueOf(78), WeightUnit.KG, OnboardingPrimaryGoal.STRENGTH, null);

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        assertThat(proposal.sessions()).isNotEmpty();
        var exerciseNames = proposal.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseName())
                .toList();
        assertThat(exerciseNames).contains("Goblet Squat", "Push-Up");
        assertThat(exerciseNames).doesNotContain("Back Squat", "Treadmill Run");
    }

    // ── onboarding generation tests ───────────────────────────────────────────

    @Test
    void onboardingGenerateInitialProposal_ReturnsNonHardcodedExercises() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                35, BigDecimal.valueOf(80), WeightUnit.KG, OnboardingPrimaryGoal.STRENGTH, null);

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        var exerciseNames = proposal.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseName())
                .toList();

        assertThat(exerciseNames)
                .isNotEmpty()
                .doesNotContain("Back Squat", "Treadmill Run");
    }

    @Test
    void onboardingGenerateInitialProposal_VerifiesAzureOpenAiProvider() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30, BigDecimal.valueOf(75), WeightUnit.KG, OnboardingPrimaryGoal.STRENGTH, null);

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        assertThat(proposal.generatedBy().provider()).isEqualTo(ProposalProvider.AZURE_OPENAI);
        assertThat(proposal.generatedBy().deployment()).isNotBlank();
    }

    @Test
    void onboardingGenerateInitialProposal_ProposalStructureIsValid() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                32, BigDecimal.valueOf(78), WeightUnit.KG, OnboardingPrimaryGoal.STRENGTH, null);

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        assertThat(proposal.attemptId()).isNotNull();
        assertThat(proposal.proposalId()).isNotNull();
        assertThat(proposal.version()).isEqualTo(1);
        assertThat(proposal.status()).isNotNull();
        assertThat(proposal.generatedBy()).isNotNull();
        assertThat(proposal.sessions()).isNotEmpty();

        proposal.sessions().forEach(session -> {
            assertThat(session.sequenceNumber()).isPositive();
            assertThat(session.name()).isNotBlank();
            assertThat(session.exercises()).isNotEmpty();
            session.exercises().forEach(exercise -> {
                assertThat(exercise.exerciseName()).isNotBlank();
                assertThat(exercise.exerciseType()).isNotNull();
            });
        });
    }

    // ── fail-fast: null/empty DTO from structured output throws BAD_GATEWAY ──

    @Test
    void onboardingGenerateInitialProposal_ThrowsOnEmptyModelOutput() {
        // When the model returns empty JSON, processOnboarding() returns a DTO with null/empty sessions
        when(chatModel.chat(anyString())).thenReturn("{\"sessions\": []}");

        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30, BigDecimal.valueOf(75), WeightUnit.KG, OnboardingPrimaryGoal.STRENGTH, null);

        assertThatThrownBy(() -> onboardingPlanGenerator.generateInitialProposal(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
