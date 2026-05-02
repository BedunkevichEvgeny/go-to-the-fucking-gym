package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.*;
import com.gymtracker.infrastructure.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * T051: Accepted program activation service.
 *
 * Orchestrates the complete workflow of accepting a proposal, activating it
 * as the user's active program, replacing any prior active program, and
 * recording the activation link back to the onboarding attempt.
 */
@Service
public class AcceptedProgramActivationService {

    private final ProgramActivationService programActivationService;
    private final ProgramReplacementPolicy replacementPolicy;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final PlanProposalRepository proposalRepository;
    private final OnboardingAuthorizationPolicy authorizationPolicy;

    public AcceptedProgramActivationService(
            ProgramActivationService programActivationService,
            ProgramReplacementPolicy replacementPolicy,
            WorkoutProgramRepository workoutProgramRepository,
            PlanProposalRepository proposalRepository,
            OnboardingAuthorizationPolicy authorizationPolicy
    ) {
        this.programActivationService = programActivationService;
        this.replacementPolicy = replacementPolicy;
        this.workoutProgramRepository = workoutProgramRepository;
        this.proposalRepository = proposalRepository;
        this.authorizationPolicy = authorizationPolicy;
    }

    /**
     * Accepts a proposal, activates it as the user's active program,
     * replaces any prior active program, and records the activation.
     *
     * @param userId authenticated user identifier
     * @param proposal the proposal to accept
     * @return the activated workout program
     * @throws IllegalStateException if proposal is not in PROPOSED state
     */
    @Transactional
    public WorkoutProgram acceptAndActivateProposal(UUID userId, PlanProposal proposal) {
        // Verify authorization
        authorizationPolicy.requireCanAcceptProposal(userId, proposal);

        // Activate the proposal as a new program
        WorkoutProgram newProgram = programActivationService.activateProposal(userId, toResponse(proposal));

        // Find and replace any existing active program
        workoutProgramRepository.findByUserIdAndStatus(userId, ProgramStatus.ACTIVE)
                .ifPresent(activeProgram -> {
                    replacementPolicy.replaceActiveProgram(activeProgram, newProgram);
                    workoutProgramRepository.save(activeProgram);
                });

        // Persist the new active program
        newProgram = workoutProgramRepository.save(newProgram);

        // Mark proposal as ACCEPTED
        proposal.setStatus(OnboardingEnums.ProposalStatus.ACCEPTED);
        proposalRepository.save(proposal);

        return newProgram;
    }

    /**
     * Converts a PlanProposal entity to the response DTO format.
     */
    private PlanProposalResponse toResponse(PlanProposal proposal) {
        // This is a simple transformation - full implementation would use mapper
        return new PlanProposalResponse(
                proposal.getAttempt().getId(),
                proposal.getId(),
                proposal.getVersion(),
                proposal.getStatus(),
                new ProfileGoalOnboardingDtos.GeneratedBy(
                        proposal.getProvider(),
                        proposal.getModelDeployment()
                ),
                java.util.Collections.emptyList() // Would be populated from proposal payload
        );
    }
}

