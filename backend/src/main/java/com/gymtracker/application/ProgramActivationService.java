package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.WorkoutProgram;
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

    public ProgramActivationService(ProgramMapperCompatibility mapperCompatibility) {
        this.mapperCompatibility = mapperCompatibility;
    }

    public WorkoutProgram activateProposal(java.util.UUID userId, PlanProposalResponse proposal) {
        // Generate a user-friendly program name based on proposal version
        String programName = String.format("AI Generated Plan v%d", proposal.version());

        // Map the proposal sessions to a 001 workout program
        return mapperCompatibility.mapToProgram(userId, programName, proposal.sessions());
    }
}

