package com.gymtracker.application;

import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.domain.*;
import org.springframework.stereotype.Component;

/**
 * Implements FR-013 onboarding ownership authorization policy.
 *
 * Ensures each user can only access their own onboarding attempts, proposals,
 * feedback, and accept operations.
 */
@Component
public class OnboardingAuthorizationPolicy {

    public void requireOwnedAttempt(java.util.UUID userId, ProfileGoalOnboardingAttempt attempt) {
        if (!attempt.getUserId().equals(userId)) {
            throw new ForbiddenException("Onboarding attempt does not belong to the authenticated user");
        }
    }

    public void requireOwnedProposal(java.util.UUID userId, PlanProposal proposal) {
        if (!proposal.getAttempt().getUserId().equals(userId)) {
            throw new ForbiddenException("Proposal does not belong to the authenticated user");
        }
    }

    public void requireOwnedFeedback(java.util.UUID userId, ProposalFeedback feedback) {
        if (!feedback.getProposal().getAttempt().getUserId().equals(userId)) {
            throw new ForbiddenException("Feedback does not belong to the authenticated user");
        }
    }

    public void requireCanAcceptProposal(java.util.UUID userId, PlanProposal proposal) {
        // User must own the proposal
        requireOwnedProposal(userId, proposal);

        // Proposal must be in PROPOSED state (not already accepted/rejected)
        if (proposal.getStatus() != OnboardingEnums.ProposalStatus.PROPOSED) {
            throw new ForbiddenException("Only proposals in PROPOSED state can be accepted");
        }
    }
}

