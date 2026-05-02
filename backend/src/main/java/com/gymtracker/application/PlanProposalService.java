package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import org.springframework.stereotype.Service;

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
}

