package com.gymtracker.application;

import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcceptedProgramActivationService {

    private final ProgramActivationService programActivationService;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final EntityManager entityManager;

    public AcceptedProgramActivationService(
            ProgramActivationService programActivationService,
            WorkoutProgramRepository workoutProgramRepository,
            EntityManager entityManager
    ) {
        this.programActivationService = programActivationService;
        this.workoutProgramRepository = workoutProgramRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public WorkoutProgram acceptProposal(UUID userId, UUID proposalId) {
        if (userId == null || proposalId == null) {
            throw new ValidationException("userId and proposalId are required");
        }

        PlanProposal proposal = entityManager.find(PlanProposal.class, proposalId);
        if (proposal == null || !userId.equals(proposal.getUserId())) {
            throw new ResourceNotFoundException("Proposal not found");
        }

        if (proposal.getStatus() == ProposalStatus.ACCEPTED) {
            return loadActivatedProgramOrFail(proposalId);
        }

        if (proposal.getStatus() != ProposalStatus.PROPOSED) {
            throw new ValidationException("Proposal is not in an acceptable state");
        }

        return programActivationService.activateAcceptedProposal(userId, proposal).activatedProgram();
    }

    private WorkoutProgram loadActivatedProgramOrFail(UUID proposalId) {
        List<AcceptedProgramActivation> activations = entityManager
                .createQuery(
                        "select a from AcceptedProgramActivation a where a.proposalId = :proposalId",
                        AcceptedProgramActivation.class)
                .setParameter("proposalId", proposalId)
                .setMaxResults(1)
                .getResultList();

        if (activations.isEmpty()) {
            throw new ValidationException("Proposal is already accepted but activation record is missing");
        }

        UUID activatedProgramId = activations.getFirst().getActivatedProgramId();
        return workoutProgramRepository.findById(activatedProgramId)
                .orElseThrow(() -> new ValidationException("Activated program not found"));
    }
}

