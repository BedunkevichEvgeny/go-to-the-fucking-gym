package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingAttemptResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.TrackingAccessGateResponse;
import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.AcceptedProgramActivationRepository;
import com.gymtracker.infrastructure.repository.PlanProposalRepository;
import com.gymtracker.infrastructure.repository.ProfileGoalOnboardingAttemptRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for PlanProposalService persistence.
 *
 * Verifies that:
 * 1. getCurrentAttempt() returns actual persisted attempt from DB
 * 2. createRevision() maintains proposal chain (same attemptId, incrementing version)
 * 3. resolveAttemptSnapshot() loads actual user inputs (not hardcoded)
 * 4. getTrackingAccessGate() validates against real acceptance status
 * 5. Reject/revise cycle maintains proposal linkage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanProposalServiceIT {

    @Autowired
    private PlanProposalService planProposalService;

    @Autowired
    private ProfileGoalOnboardingAttemptRepository attemptRepository;

    @Autowired
    private PlanProposalRepository proposalRepository;

    @Autowired
    private AcceptedProgramActivationRepository activationRepository;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getCurrentAttempt_ReturnsEmptyWhenNoAttempt() {
        Optional<OnboardingAttemptResponse> result = planProposalService.getCurrentAttempt(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentAttempt_ReturnsPersistedAttemptAfterCreation() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                35,
                BigDecimal.valueOf(80),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        // Create initial proposal
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);

        // ✅ Query for current attempt
        Optional<OnboardingAttemptResponse> result = planProposalService.getCurrentAttempt(userId);

        assertThat(result).isPresent();
        assertThat(result.get().attemptId()).isNotNull();
        assertThat(result.get().latestProposal().version()).isEqualTo(1);
        assertThat(result.get().latestProposal().proposalId()).isEqualTo(v1.proposalId());
    }

    @Test
    void createRevision_MaintainsProposalChain() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30,
                BigDecimal.valueOf(75),
                WeightUnit.KG,
                OnboardingPrimaryGoal.BUILD_MUSCLES,
                null
        );

        // Create initial proposal (v1)
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);

        // Reject and revise to create v2
        PlanProposalResponse v2 = planProposalService.createRevision(userId, v1.proposalId(), "Make it harder");

        // ✅ Same attempt ID (proposal chain continuity)
        assertThat(v2.attemptId()).isEqualTo(v1.attemptId());

        // ✅ Different proposal IDs (linked chain, not replacement)
        assertThat(v2.proposalId()).isNotEqualTo(v1.proposalId());

        // ✅ Version incremented
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.status()).isEqualTo(ProposalStatus.PROPOSED);
    }

    @Test
    void createRevision_CreatesThirdVersion() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                28,
                BigDecimal.valueOf(70),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);
        PlanProposalResponse v2 = planProposalService.createRevision(userId, v1.proposalId(), "Add more cardio");
        PlanProposalResponse v3 = planProposalService.createRevision(userId, v2.proposalId(), "Make it moderate");

        // ✅ All three share same attempt ID
        assertThat(v3.attemptId()).isEqualTo(v1.attemptId()).isEqualTo(v2.attemptId());

        // ✅ All three have different proposal IDs
        assertThat(v3.proposalId()).isNotEqualTo(v1.proposalId()).isNotEqualTo(v2.proposalId());

        // ✅ Versions increment: 1 → 2 → 3
        assertThat(v3.version()).isEqualTo(3);
    }

    @Test
    void resolveAttemptSnapshot_LoadsActualUserInputs() {
        OnboardingSubmissionRequest originalRequest = new OnboardingSubmissionRequest(
                42,
                BigDecimal.valueOf(85),
                WeightUnit.LBS,
                OnboardingPrimaryGoal.LOSE_WEIGHT,
                null
        );

        // Create initial proposal
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, originalRequest);
        UUID attemptId = v1.attemptId();

        // ✅ Resolve the snapshot - should return the original request
        OnboardingSubmissionRequest resolved = planProposalService.resolveAttemptSnapshot(userId, attemptId);

        assertThat(resolved.age()).isEqualTo(42);
        assertThat(resolved.currentWeight()).isEqualByComparingTo(BigDecimal.valueOf(85));
        assertThat(resolved.weightUnit()).isEqualTo(WeightUnit.LBS);
        assertThat(resolved.primaryGoal()).isEqualTo(OnboardingPrimaryGoal.LOSE_WEIGHT);
    }

    @Test
    void resolveAttemptSnapshot_NotHardcoded() {
        OnboardingSubmissionRequest request1 = new OnboardingSubmissionRequest(
                25,
                BigDecimal.valueOf(60),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request1);

        OnboardingSubmissionRequest resolved = planProposalService.resolveAttemptSnapshot(userId, v1.attemptId());

        // ✅ NOT the hardcoded values (30, 75, KG, STRENGTH)
        // Should be the actual request
        assertThat(resolved.age()).isEqualTo(25);
        assertThat(resolved.currentWeight()).isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    @Test
    void getTrackingAccessGate_DeniesBeforeAcceptance() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30,
                BigDecimal.valueOf(75),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        // Create proposal but don't accept
        planProposalService.createInitialProposal(userId, request);

        // ✅ Should be denied (onboarding not complete)
        TrackingAccessGateResponse gate = planProposalService.getTrackingAccessGate(userId);

        assertThat(gate.canAccessProgramTracking()).isFalse();
        assertThat(gate.reasonCode()).isEqualTo("ONBOARDING_REQUIRED");
        assertThat(gate.currentAttemptId()).isNotNull();
    }

    @Test
    void getTrackingAccessGate_AllowsAfterAcceptance() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30,
                BigDecimal.valueOf(75),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        // Create and accept proposal
        PlanProposalResponse proposal = planProposalService.createInitialProposal(userId, request);

        // Simulate acceptance by creating an accepted activation record
        AcceptedProgramActivation activation = new AcceptedProgramActivation();
        activation.setId(UUID.randomUUID());
        activation.setProposalId(proposal.proposalId());
        activation.setUserId(userId);
        activation.setAttemptId(proposal.attemptId());
        activation.setActivatedProgramId(UUID.randomUUID());
        activationRepository.save(activation);

        // ✅ Should be allowed (onboarding accepted)
        TrackingAccessGateResponse gate = planProposalService.getTrackingAccessGate(userId);

        assertThat(gate.canAccessProgramTracking()).isTrue();
        assertThat(gate.reasonCode()).isEqualTo("ALLOWED");
    }

    @Test
    void getTrackingAccessGate_NewUserBlocked() {
        UUID newUserId = UUID.randomUUID();

        // ✅ New user with no onboarding attempt should be blocked
        TrackingAccessGateResponse gate = planProposalService.getTrackingAccessGate(newUserId);

        assertThat(gate.canAccessProgramTracking()).isFalse();
        assertThat(gate.reasonCode()).isEqualTo("ONBOARDING_REQUIRED");
    }

    @Test
    void rejectAndReviseFlow_MaintainsContinuity() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                33,
                BigDecimal.valueOf(82),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null
        );

        // Step 1: Create initial proposal
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);
        UUID attemptId = v1.attemptId();
        UUID proposalV1Id = v1.proposalId();

        // Step 2: Reject and ask for revision
        PlanProposalResponse v2 = planProposalService.createRevision(
                userId,
                proposalV1Id,
                "Less strength, more cardio"
        );
        UUID proposalV2Id = v2.proposalId();

        // Step 3: Query current attempt - should show v2
        Optional<OnboardingAttemptResponse> currentAttempt = planProposalService.getCurrentAttempt(userId);
        assertThat(currentAttempt).isPresent();
        assertThat(currentAttempt.get().latestProposal().version()).isEqualTo(2);
        assertThat(currentAttempt.get().latestProposal().proposalId()).isEqualTo(proposalV2Id);

        // Step 4: Reject again and revise
        PlanProposalResponse v3 = planProposalService.createRevision(
                userId,
                proposalV2Id,
                "Add flexibility training"
        );

        // ✅ All proposals linked via same attempt ID
        assertThat(v1.attemptId()).isEqualTo(v2.attemptId()).isEqualTo(v3.attemptId()).isEqualTo(attemptId);

        // ✅ Different proposal IDs forming a chain
        assertThat(v1.proposalId()).isNotEqualTo(v2.proposalId()).isNotEqualTo(v3.proposalId());

        // ✅ Versions increment correctly
        assertThat(v1.version()).isEqualTo(1);
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v3.version()).isEqualTo(3);
    }
}
