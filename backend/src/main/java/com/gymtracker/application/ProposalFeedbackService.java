package com.gymtracker.application;

import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProposalFeedback;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProposalFeedbackService {

    private final EntityManager entityManager;
    private final Clock clock;

    public ProposalFeedbackService() {
        this(null, null);
    }

    public ProposalFeedbackService(EntityManager entityManager) {
        this(entityManager, Clock.systemUTC());
    }

    ProposalFeedbackService(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public void validateFeedback(UUID userId, UUID proposalId, String requestedChanges) {
        if (proposalId == null || userId == null) {
            throw new ValidationException("userId and proposalId are required");
        }
        if (requestedChanges == null || requestedChanges.trim().isEmpty()) {
            throw new ValidationException("requestedChanges is required");
        }
        if (requestedChanges.length() > 2000) {
            throw new ValidationException("requestedChanges must be <= 2000 characters");
        }
    }

    @Transactional
    public void storeFeedback(UUID userId, UUID proposalId, String requestedChanges) {
        validateFeedback(userId, proposalId, requestedChanges);
        if (entityManager == null) {
            return;
        }

        PlanProposal proposal = entityManager.find(PlanProposal.class, proposalId);
        if (proposal == null || !userId.equals(proposal.getUserId())) {
            return;
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        entityManager.merge(proposal);

        ProposalFeedback feedback = ProposalFeedback.builder()
                .attemptId(proposal.getAttempt().getId())
                .proposal(proposal)
                .userId(userId)
                .requestedChanges(requestedChanges.trim())
                .createdAt(OffsetDateTime.now(clock))
                .build();
        entityManager.persist(feedback);
    }
}
