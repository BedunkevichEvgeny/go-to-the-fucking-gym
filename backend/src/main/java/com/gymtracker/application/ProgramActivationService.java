package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.mapper.OnboardingProposalMapper;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Activates accepted proposals as active workout programs.
 *
 * Responsible for converting a proposal accepted by the user into a persisted
 * WorkoutProgram that is immediately available for use in 001 program session
 * logging and tracking workflows.
 */
@Service
public class ProgramActivationService {

    private final ProgramMapperCompatibility mapperCompatibility;
    private final OnboardingProposalMapper onboardingProposalMapper;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final EntityManager entityManager;

    @Autowired
    public ProgramActivationService(
            ProgramMapperCompatibility mapperCompatibility,
            OnboardingProposalMapper onboardingProposalMapper,
            WorkoutProgramRepository workoutProgramRepository,
            EntityManager entityManager
    ) {
        this.mapperCompatibility = mapperCompatibility;
        this.onboardingProposalMapper = onboardingProposalMapper;
        this.workoutProgramRepository = workoutProgramRepository;
        this.entityManager = entityManager;
    }

    // Secondary constructor kept for isolated mapper unit tests.
    ProgramActivationService(ProgramMapperCompatibility mapperCompatibility) {
        this(mapperCompatibility, null, null, null);
    }

    public WorkoutProgram activateProposal(java.util.UUID userId, PlanProposalResponse proposal) {
        if (userId == null || proposal == null) {
            throw new ValidationException("userId and proposal are required");
        }

        // Generate a user-friendly program name based on proposal version
        String programName = String.format("AI Generated Plan v%d", proposal.version());

        // Map the proposal sessions to a 001 workout program
        return mapperCompatibility.mapToProgram(userId, programName, proposal.sessions());
    }

    @Transactional
    public ActivationResult activateAcceptedProposal(UUID userId, PlanProposal proposal) {
        if (userId == null || proposal == null) {
            throw new ValidationException("userId and proposal are required");
        }
        if (!userId.equals(proposal.getUserId())) {
            throw new ValidationException("Proposal does not belong to the authenticated user");
        }

        WorkoutProgram replacedProgram = deactivateCurrentPrograms(userId).orElse(null);

        PlanProposalResponse proposalResponse = onboardingProposalMapper.toResponse(proposal);
        WorkoutProgram activatedProgram = workoutProgramRepository.save(activateProposal(userId, proposalResponse));

        proposal.setStatus(ProposalStatus.ACCEPTED);
        entityManager.merge(proposal);

        proposal.getAttempt().setStatus(OnboardingAttemptStatus.ACCEPTED);
        entityManager.merge(proposal.getAttempt());

        persistActivationRecord(proposal, userId, activatedProgram.getId(), replacedProgram);
        return new ActivationResult(activatedProgram, replacedProgram == null ? null : replacedProgram.getId());
    }

    private Optional<WorkoutProgram> deactivateCurrentPrograms(UUID userId) {
        List<WorkoutProgram> activePrograms = entityManager
                .createQuery(
                        "select p from WorkoutProgram p where p.userId = :userId and p.status = :status",
                        WorkoutProgram.class)
                .setParameter("userId", userId)
                .setParameter("status", ProgramStatus.ACTIVE)
                .getResultList();

        if (activePrograms.isEmpty()) {
            return Optional.empty();
        }

        OffsetDateTime replacedAt = OffsetDateTime.now();
        for (WorkoutProgram program : activePrograms) {
            program.setStatus(ProgramStatus.REPLACED);
            program.setCompletedAt(replacedAt);
            workoutProgramRepository.save(program);
        }

        return activePrograms.stream()
                .max(Comparator.comparing(WorkoutProgram::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private void persistActivationRecord(
            PlanProposal proposal,
            UUID userId,
            UUID activatedProgramId,
            WorkoutProgram replacedProgram
    ) {
        List<AcceptedProgramActivation> existing = entityManager
                .createQuery(
                        "select a from AcceptedProgramActivation a where a.proposalId = :proposalId",
                        AcceptedProgramActivation.class)
                .setParameter("proposalId", proposal.getId())
                .setMaxResults(1)
                .getResultList();

        if (!existing.isEmpty()) {
            return;
        }

        AcceptedProgramActivation activation = AcceptedProgramActivation.builder()
                .attemptId(proposal.getAttempt().getId())
                .proposalId(proposal.getId())
                .userId(userId)
                .activatedProgramId(activatedProgramId)
                .replacedProgramId(replacedProgram == null ? null : replacedProgram.getId())
                .activatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(activation);
    }

    public record ActivationResult(WorkoutProgram activatedProgram, UUID replacedProgramId) {
    }
}

