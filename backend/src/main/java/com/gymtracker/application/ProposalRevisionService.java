package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposalRejectRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProposalRevisionService {

    private final PlanProposalService planProposalService;
    private final ProposalFeedbackService proposalFeedbackService;

    public ProposalRevisionService(
            PlanProposalService planProposalService,
            ProposalFeedbackService proposalFeedbackService
    ) {
        this.planProposalService = planProposalService;
        this.proposalFeedbackService = proposalFeedbackService;
    }

    public PlanProposalResponse rejectAndRevise(UUID userId, UUID proposalId, ProposalRejectRequest request) {
        proposalFeedbackService.storeFeedback(userId, proposalId, request.requestedChanges());
        return planProposalService.createRevision(userId, proposalId, request.requestedChanges());
    }

    public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
        return planProposalService.resolveAttemptSnapshot(userId, attemptId);
    }
}

