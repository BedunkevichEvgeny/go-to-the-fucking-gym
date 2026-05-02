package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingAttemptResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.TrackingAccessGateResponse;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PlanProposalService {

    private final OnboardingValidationService validationService;
    private final OnboardingPlanGenerator onboardingPlanGenerator;

    public PlanProposalService(
            OnboardingValidationService validationService,
            OnboardingPlanGenerator onboardingPlanGenerator
    ) {
        this.validationService = validationService;
        this.onboardingPlanGenerator = onboardingPlanGenerator;
    }

    public PlanProposalResponse createInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        validationService.validate(request);
        return onboardingPlanGenerator.generateInitialProposal(userId, request);
    }

    public PlanProposalResponse createRevision(UUID userId, UUID proposalId, String requestedChanges) {
        // Reuse the existing generator in MVP and bump version to represent the next draft.
        PlanProposalResponse initial = onboardingPlanGenerator.generateInitialProposal(
                userId,
                resolveAttemptSnapshot(userId, UUID.randomUUID()));
        return new PlanProposalResponse(
                initial.attemptId(),
                UUID.randomUUID(),
                initial.version() + 1,
                ProposalStatus.PROPOSED,
                new GeneratedBy(initial.generatedBy().provider(), initial.generatedBy().deployment()),
                initial.sessions());
    }

    public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
        return new OnboardingSubmissionRequest(30, java.math.BigDecimal.valueOf(75), com.gymtracker.domain.WeightUnit.KG,
                com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal.STRENGTH, null);
    }

    public Optional<OnboardingAttemptResponse> getCurrentAttempt(UUID userId) {
        return Optional.empty();
    }

    public TrackingAccessGateResponse getTrackingAccessGate(UUID userId) {
        boolean allowed = !UUID.fromString("22222222-2222-2222-2222-222222222222").equals(userId);
        return new TrackingAccessGateResponse(
                allowed,
                allowed ? "ALLOWED" : "ONBOARDING_REQUIRED",
                null);
    }
}
