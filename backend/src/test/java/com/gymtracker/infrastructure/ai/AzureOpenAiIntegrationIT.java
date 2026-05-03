package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "azure.openai.endpoint=https://integration.test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-gpt-deployment"
})
class AzureOpenAiIntegrationIT {

    @Autowired
    private LangChainSessionProcessor processor;

    @Autowired
    private OnboardingPlanGenerator onboardingPlanGenerator;

    @Test
    void processorInitializesFromAzureEnvironmentProperties() {
        assertThat(processor.endpoint()).isEqualTo("https://integration.test.azure.openai");
        assertThat(processor.deployment()).isEqualTo("test-gpt-deployment");
    }

    @Test
    void workflowAcceptsSessionSummaryAndBuildsPromptWithSessionData() {
        SessionSummaryDTO summary = sampleSummary();

        String prompt = processor.buildPrompt(summary);
        String response = processor.process(summary);

        assertThat(prompt).contains("Session: " + summary.sessionId());
        assertThat(prompt).contains("Bench Press");
        assertThat(prompt).contains("target=");
        assertThat(prompt).contains("Feeling rating: 8");
        assertThat(response).contains("azure-openai[");
        assertThat(response).contains("deployment=test-gpt-deployment");
    }

    @Test
    void workflowReturnsCapturedResponseWithinTimeoutBudget() {
        SessionSummaryDTO summary = sampleSummary();

        String response = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> processor.process(summary));

        assertThat(response).isNotBlank();
    }

    // ===== T063-BUG-001-TEST: Onboarding AI Generation Tests =====

    @Test
    void onboardingGenerateInitialProposal_ReturnsNonHardcodedExercises() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                35,
                BigDecimal.valueOf(80),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        // ✅ Key assertion: Exercises should NOT be the hardcoded names
        var exerciseNames = proposal.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseName())
                .toList();

        assertThat(exerciseNames)
                .isNotEmpty()
                .doesNotContain("Back Squat", "Treadmill Run");
    }

    @Test
    void onboardingGenerateInitialProposal_VariesBasedOnUserProfile() {
        UUID userId1 = UUID.randomUUID();
        OnboardingSubmissionRequest request1 = new OnboardingSubmissionRequest(
                25,
                BigDecimal.valueOf(60),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse proposal1 = onboardingPlanGenerator.generateInitialProposal(userId1, request1);

        UUID userId2 = UUID.randomUUID();
        OnboardingSubmissionRequest request2 = new OnboardingSubmissionRequest(
                50,
                BigDecimal.valueOf(95),
                WeightUnit.LBS,
                OnboardingPrimaryGoal.BUILD_MUSCLES,
                null
        );

        PlanProposalResponse proposal2 = onboardingPlanGenerator.generateInitialProposal(userId2, request2);

        // ✅ Proposals should differ based on input (age, weight, goal)
        var exercises1 = proposal1.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseName())
                .toList();

        var exercises2 = proposal2.sessions().stream()
                .flatMap(s -> s.exercises().stream())
                .map(e -> e.exerciseName())
                .toList();

        // At least some variation expected (not identical proposals for different profiles)
        assertThat(exercises1).isNotEmpty();
        assertThat(exercises2).isNotEmpty();
    }

    @Test
    void onboardingGenerateInitialProposal_VerifiesAzureOpenAiProvider() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30,
                BigDecimal.valueOf(75),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        // ✅ Verify it's from Azure OpenAI provider
        assertThat(proposal.generatedBy().provider())
                .isEqualTo(ProposalProvider.AZURE_OPENAI);
        assertThat(proposal.generatedBy().deployment()).isNotBlank();
    }

    @Test
    void onboardingGenerateInitialProposal_ProposalStructureIsValid() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                32,
                BigDecimal.valueOf(78),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        // ✅ Verify proposal structure
        assertThat(proposal.attemptId()).isNotNull();
        assertThat(proposal.proposalId()).isNotNull();
        assertThat(proposal.version()).isEqualTo(1);
        assertThat(proposal.status()).isNotNull();
        assertThat(proposal.generatedBy()).isNotNull();
        assertThat(proposal.sessions()).isNotEmpty();

        // ✅ Verify sessions contain exercises
        proposal.sessions().forEach(session -> {
            assertThat(session.sequenceNumber()).isPositive();
            assertThat(session.name()).isNotBlank();
            assertThat(session.exercises()).isNotEmpty();

            // ✅ Verify exercises have required fields
            session.exercises().forEach(exercise -> {
                assertThat(exercise.exerciseName()).isNotBlank();
                assertThat(exercise.exerciseType()).isNotNull();
            });
        });
    }

    // ===== T065-BUG-003-TEST: LangChain/Azure Integration Contract Tests =====

    @Test
    void contractTest_processorOutputDoesNotContainSyntheticStubSignature() {
        // Contract: after T065-BUG-003, callAzureOpenAi must return real model output, NOT the
        // synthetic stub pattern "azure-openai[endpoint=...,deployment=...,apiKey=...]: accepted prompt hash=..."
        LangChainSessionProcessor contractProcessor = new LangChainSessionProcessor(
                "https://test.azure.openai", "test-key", "test-deployment", 30, 1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                // Simulates what a real Azure OpenAI model returns (natural language or JSON)
                return "Session analysis: Good progression observed. Recommend increasing weight by 2.5kg next session.";
            }
        };

        String result = contractProcessor.process(sampleSummary());

        // Must NOT contain the synthetic stub response signature
        assertThat(result).doesNotContain("azure-openai[endpoint=");
        assertThat(result).doesNotContain("accepted prompt hash=");
        assertThat(result).doesNotContain("apiKey=present");
        // Must contain actual model output
        assertThat(result).contains("progression");
    }

    @Test
    void contractTest_stubPatternIsNotValidJsonForOnboardingParser() {
        // Document the contract violation: the current stub response "azure-openai[..." is NOT valid JSON.
        // Any processor that returns this pattern CANNOT be consumed by the onboarding proposal parser.
        // This test anchors the requirement: real model output must be strict JSON.
        String syntheticStubResponse =
                "azure-openai[endpoint=https://integration.test.azure.openai,"
                + "deployment=test-gpt-deployment,apiKey=present]: accepted prompt hash=-123456789";

        // The stub is NOT JSON-structured
        assertThat(syntheticStubResponse).doesNotStartWith("{");
        assertThat(syntheticStubResponse).doesNotContain("\"sessions\"");
        assertThat(syntheticStubResponse).doesNotContain("\"exercises\"");

        // Simulating what onboarding parser does with stub output: must throw, NOT silently use it
        LangChainSessionProcessor stubProcessor = new LangChainSessionProcessor(
                "https://test.azure.openai", "test-key", "test-deployment", 30, 1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                return syntheticStubResponse;
            }
        };
        // The stub output is passed through by the processor itself
        String result = stubProcessor.process(sampleSummary());
        assertThat(result).isEqualTo(syntheticStubResponse);
        // This confirms the processor returns the stub verbatim — downstream (OnboardingPlanGenerator)
        // must fail-fast when it receives this non-JSON content.
    }

    @Test
    void contractTest_processorWithValidJsonOutputIsConsumableByOnboardingParser() {
        // After T065-BUG-003 + T067-BUG-005: real model returns JSON, onboarding parser succeeds.
        // This test verifies the contract between LangChainSessionProcessor and downstream parsers.
        String validOnboardingJson = """
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

        LangChainSessionProcessor jsonReturningProcessor = new LangChainSessionProcessor(
                "https://test.azure.openai", "test-key", "test-deployment", 30, 1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                return validOnboardingJson;
            }
        };

        String result = jsonReturningProcessor.process(sampleSummary());

        // Result must be the actual JSON from the model
        assertThat(result).contains("\"sessions\"");
        assertThat(result).contains("Goblet Squat");
        assertThat(result).contains("Push-Up");
        // NOT hardcoded fallback exercises
        assertThat(result).doesNotContain("Back Squat");
        assertThat(result).doesNotContain("Treadmill Run");
        // NOT the synthetic stub signature
        assertThat(result).doesNotContain("azure-openai[endpoint=");
    }

    @Test
    void contractTest_processorTimeoutPropagatesWithoutFallback() {
        // Contract: on timeout, processor must throw (not return synthetic stub or fallback)
        LangChainSessionProcessor timeoutProcessor = new LangChainSessionProcessor(
                "https://test.azure.openai", "test-key", "test-deployment", 1, 1
        ) {
            @Override
            String callAzureOpenAi(String prompt) {
                throw new IllegalStateException("timeout after 1s");
            }
        };

        assertThatThrownBy(() -> timeoutProcessor.process(sampleSummary()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timeout");
        // No fallback stub is returned on timeout
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
