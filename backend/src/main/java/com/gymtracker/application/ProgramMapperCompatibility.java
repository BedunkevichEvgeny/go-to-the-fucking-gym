package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedExerciseTarget;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.WorkoutProgram;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps onboarding proposal sessions to 001 workout program entities.
 *
 * Responsible for converting the structured proposal output (sessions + exercises)
 * into WorkoutProgram, ProgramSession, and ProgramExerciseTarget entities that are
 * immediately compatible with existing 001 program session logging and tracking behavior.
 */
@Component
public class ProgramMapperCompatibility {

    public WorkoutProgram mapToProgram(UUID userId, String programName, List<ProposedSession> proposedSessions) {
        List<ProgramSession> sessions = new ArrayList<>();

        for (ProposedSession proposed : proposedSessions) {
            ProgramSession programSession = mapToSession(proposed);
            sessions.add(programSession);
        }

        WorkoutProgram program = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(programName)
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .sessions(sessions)
                .build();

        // Link sessions to program
        for (ProgramSession session : sessions) {
            session.setProgram(program);
        }

        return program;
    }

    private ProgramSession mapToSession(ProposedSession proposed) {
        List<ProgramExerciseTarget> targets = new ArrayList<>();

        for (int i = 0; i < proposed.exercises().size(); i++) {
            ProposedExerciseTarget proposedExercise = proposed.exercises().get(i);
            ProgramExerciseTarget target = mapToTarget(proposedExercise, i);
            targets.add(target);
        }

        ProgramSession session = ProgramSession.builder()
                .id(UUID.randomUUID())
                .sequenceNumber(proposed.sequenceNumber())
                .name(proposed.name())
                .completed(false)
                .exerciseTargets(targets)
                .build();

        // Link targets to session
        for (ProgramExerciseTarget target : targets) {
            target.setProgramSession(session);
        }

        return session;
    }

    private ProgramExerciseTarget mapToTarget(ProposedExerciseTarget proposed, int sortOrder) {
        return ProgramExerciseTarget.builder()
                .id(UUID.randomUUID())
                .exerciseName(proposed.exerciseName())
                .exerciseType(proposed.exerciseType())
                .targetSets(proposed.targetSets())
                .targetReps(proposed.targetReps())
                .targetWeight(proposed.targetWeight())
                .targetWeightUnit(proposed.targetWeightUnit())
                .targetDurationSeconds(proposed.targetDurationSeconds())
                .targetDistance(proposed.targetDistance())
                .targetDistanceUnit(proposed.targetDistanceUnit())
                .sortOrder(sortOrder)
                .build();
    }
}


