package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.mapper.OnboardingProposalMapper;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

@Service
public class AcceptedProgramActivationService {

    private final ProgramActivationService programActivationService;
    private final OnboardingProposalMapper onboardingProposalMapper;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final EntityManager entityManager;

    public AcceptedProgramActivationService(
            ProgramActivationService programActivationService,
            OnboardingProposalMapper onboardingProposalMapper,
            WorkoutProgramRepository workoutProgramRepository,
            EntityManager entityManager
    ) {
        this.programActivationService = programActivationService;
        this.onboardingProposalMapper = onboardingProposalMapper;
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

        WorkoutProgram replacedProgram = deactivateCurrentPrograms(userId);
        PlanProposalResponse proposalResponse = onboardingProposalMapper.toResponse(proposal);
        WorkoutProgram activatedProgram = workoutProgramRepository.save(
                programActivationService.activateProposal(userId, proposalResponse));

        proposal.setStatus(ProposalStatus.ACCEPTED);
        entityManager.merge(proposal);

        ProfileGoalOnboardingAttempt attempt = proposal.getAttempt();
        attempt.setStatus(OnboardingAttemptStatus.ACCEPTED);
        entityManager.merge(attempt);

        persistActivationRecord(attempt.getId(), proposal.getId(), userId, activatedProgram.getId(), replacedProgram);

        return activatedProgram;
    }

    @Nullable
    private WorkoutProgram deactivateCurrentPrograms(UUID userId) {
        List<WorkoutProgram> activePrograms = entityManager
                .createQuery(
                        "select p from WorkoutProgram p where p.userId = :userId and p.status = :status",
                        WorkoutProgram.class)
                .setParameter("userId", userId)
                .setParameter("status", ProgramStatus.ACTIVE)
                .getResultList();

        if (activePrograms.isEmpty()) {
            return null;
        }

        OffsetDateTime replacedAt = OffsetDateTime.now();
        activePrograms.forEach(program -> {
            program.setStatus(ProgramStatus.REPLACED);
            program.setCompletedAt(replacedAt);
            workoutProgramRepository.save(program);
        });

        return activePrograms.stream()
                .max(Comparator.comparing(WorkoutProgram::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(activePrograms.getFirst());
    }

    private void persistActivationRecord(
            UUID attemptId,
            UUID proposalId,
            UUID userId,
            UUID activatedProgramId,
            @Nullable
            WorkoutProgram replacedProgram
    ) {
        List<AcceptedProgramActivation> existing = entityManager
                .createQuery(
                        "select a from AcceptedProgramActivation a where a.proposalId = :proposalId",
                        AcceptedProgramActivation.class)
                .setParameter("proposalId", proposalId)
                .setMaxResults(1)
                .getResultList();

        if (!existing.isEmpty()) {
            return;
        }

        UUID replacedProgramId = replacedProgram != null ? replacedProgram.getId() : null;
        AcceptedProgramActivation activation = AcceptedProgramActivation.builder()
                .attemptId(attemptId)
                .proposalId(proposalId)
                .userId(userId)
                .activatedProgramId(activatedProgramId)
                .replacedProgramId(replacedProgramId)
                .activatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(activation);
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

