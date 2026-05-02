package com.gymtracker.application;

import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * T048: Unit tests for FR-013 onboarding ownership authorization policy.
 *
 * Verifies that users can only access their own onboarding attempts,
 * proposals, feedback, and accept operations.
 */
class OnboardingAuthorizationPolicyTest {

    private OnboardingAuthorizationPolicy authorizationPolicy;

    @BeforeEach
    void setUp() {
        authorizationPolicy = new OnboardingAuthorizationPolicy();
    }

    @Test
    void userCanAccessTheirOwnAttempt() {
        // Given: a user and their onboarding attempt
        UUID userId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        // When: checking authorization
        // Then: should be allowed
        assertThatNoException().isThrownBy(() ->
                authorizationPolicy.requireOwnedAttempt(userId, attempt)
        );
    }

    @Test
    void userCannotAccessAnotherUsersAttempt() {
        // Given: attempt belongs to different user
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        // When: different user tries to access
        // Then: should be forbidden
        assertThatThrownBy(() ->
                authorizationPolicy.requireOwnedAttempt(userId, attempt)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void userCanAccessTheirOwnProposal() {
        // Given: proposal created by user
        UUID userId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        // When: accessing their proposal
        // Then: should be allowed
        assertThatNoException().isThrownBy(() ->
                authorizationPolicy.requireOwnedProposal(userId, proposal)
        );
    }

    @Test
    void userCannotAccessAnotherUsersProposal() {
        // Given: proposal from another user
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        // When: different user tries to access
        // Then: should be forbidden
        assertThatThrownBy(() ->
                authorizationPolicy.requireOwnedProposal(userId, proposal)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void userCanAccessTheirOwnFeedback() {
        // Given: feedback from user's proposal
        UUID userId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        ProposalFeedback feedback = ProposalFeedback.builder()
                .id(UUID.randomUUID())
                .proposal(proposal)
                .requestedChanges("More cardio")
                .build();

        // When: accessing their feedback
        // Then: should be allowed
        assertThatNoException().isThrownBy(() ->
                authorizationPolicy.requireOwnedFeedback(userId, feedback)
        );
    }

    @Test
    void userCannotAccessAnotherUsersFeedback() {
        // Given: feedback from another user's proposal
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        ProposalFeedback feedback = ProposalFeedback.builder()
                .id(UUID.randomUUID())
                .proposal(proposal)
                .requestedChanges("Changes")
                .build();

        // When: different user tries to access
        // Then: should be forbidden
        assertThatThrownBy(() ->
                authorizationPolicy.requireOwnedFeedback(userId, feedback)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void userCanAcceptTheirOwnProposal() {
        // Given: user's proposal in PROPOSED state
        UUID userId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        // When: accepting their proposal
        // Then: should be allowed
        assertThatNoException().isThrownBy(() ->
                authorizationPolicy.requireCanAcceptProposal(userId, proposal)
        );
    }

    @Test
    void userCannotAcceptAnotherUsersProposal() {
        // Given: proposal from another user
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        // When: different user tries to accept
        // Then: should be forbidden
        assertThatThrownBy(() ->
                authorizationPolicy.requireCanAcceptProposal(userId, proposal)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void userCannotAcceptAlreadyAcceptedProposal() {
        // Given: proposal already in ACCEPTED state
        UUID userId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        PlanProposal proposal = PlanProposal.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.ACCEPTED)
                .proposalPayload("{}")
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();

        // When: trying to accept again
        // Then: should be forbidden (already accepted)
        assertThatThrownBy(() ->
                authorizationPolicy.requireCanAcceptProposal(userId, proposal)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorizationErrorMessageIsClear() {
        // Given: denied access scenario
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .age(30)
                .currentWeight(BigDecimal.valueOf(75))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();

        // When: access is denied
        // Then: error message should be informative
        var ex = catchThrowableOfType(
                () -> authorizationPolicy.requireOwnedAttempt(userId, attempt),
                ForbiddenException.class
        );
        assertThat(ex.getMessage()).contains("onboarding");
    }
}

