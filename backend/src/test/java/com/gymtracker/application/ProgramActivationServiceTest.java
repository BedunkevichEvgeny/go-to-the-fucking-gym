package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.*;
import com.gymtracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * T040: Unit tests for accepted proposal activation service.
 *
 * Verifies that accepting a proposal correctly activates it as the user's active program
 * and handles the workflow of converting a proposal to a persisted workout program.
 */
class ProgramActivationServiceTest {

    private ProgramActivationService activationService;
    private ProgramMapperCompatibility mapperCompatibility;

    @BeforeEach
    void setUp() {
        mapperCompatibility = new ProgramMapperCompatibility();
        activationService = new ProgramActivationService(mapperCompatibility);
    }

    @Test
    void activateProposalCreatesWorkoutProgram() {
        // Given a proposal with sessions and exercises
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedExerciseTarget benchPress = new ProposedExerciseTarget(
                "Bench Press",
                ExerciseType.STRENGTH,
                3,
                8,
                BigDecimal.valueOf(100),
                WeightUnit.KG,
                null,
                null,
                null
        );
        ProposedSession session = new ProposedSession(1, "Upper Body", List.of(benchPress));
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating the proposal
        WorkoutProgram activatedProgram = activationService.activateProposal(userId, proposal);

        // Then the program should be created and active
        assertThat(activatedProgram).isNotNull();
        assertThat(activatedProgram.getUserId()).isEqualTo(userId);
        assertThat(activatedProgram.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(activatedProgram.getSessions()).hasSize(1);
        assertThat(activatedProgram.getCreatedAt()).isNotNull();
    }

    @Test
    void activateProposalPreservesSessionStructure() {
        // Given a proposal with multiple sessions
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedSession session1 = new ProposedSession(
                1,
                "Day 1",
                List.of(new ProposedExerciseTarget(
                        "Squat", ExerciseType.STRENGTH, 5, 5,
                        BigDecimal.valueOf(150), WeightUnit.KG, null, null, null
                ))
        );
        ProposedSession session2 = new ProposedSession(
                2,
                "Day 2",
                List.of(new ProposedExerciseTarget(
                        "Bench", ExerciseType.STRENGTH, 4, 6,
                        BigDecimal.valueOf(100), WeightUnit.KG, null, null, null
                ))
        );
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session1, session2)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);

        // Then both sessions should be present
        assertThat(program.getSessions()).hasSize(2);
        assertThat(program.getSessions().get(0).getName()).isEqualTo("Day 1");
        assertThat(program.getSessions().get(1).getName()).isEqualTo("Day 2");
    }

    @Test
    void activateProposalPreservesExerciseMetadata() {
        // Given a proposal with specific exercise data
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedExerciseTarget dumbells = new ProposedExerciseTarget(
                "Dumbbell Curl",
                ExerciseType.STRENGTH,
                4,
                10,
                BigDecimal.valueOf(25),
                WeightUnit.KG,
                null,
                null,
                null
        );
        ProposedSession session = new ProposedSession(1, "Arms", List.of(dumbells));
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);
        ProgramExerciseTarget target = program.getSessions().get(0).getExerciseTargets().get(0);

        // Then exercise metadata should be preserved
        assertThat(target.getExerciseName()).isEqualTo("Dumbbell Curl");
        assertThat(target.getTargetSets()).isEqualTo(4);
        assertThat(target.getTargetReps()).isEqualTo(10);
        assertThat(target.getTargetWeight()).isEqualByComparingTo(BigDecimal.valueOf(25));
        assertThat(target.getTargetWeightUnit()).isEqualTo(WeightUnit.KG);
    }

    @Test
    void activateProposalHandlesCardioExercises() {
        // Given a proposal with cardio exercise
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedExerciseTarget run = new ProposedExerciseTarget(
                "Treadmill Run",
                ExerciseType.CARDIO,
                null,
                null,
                null,
                null,
                600,
                null,
                null
        );
        ProposedSession session = new ProposedSession(1, "Cardio", List.of(run));
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);
        ProgramExerciseTarget target = program.getSessions().get(0).getExerciseTargets().get(0);

        // Then cardio should be correctly preserved
        assertThat(target.getExerciseType()).isEqualTo(ExerciseType.CARDIO);
        assertThat(target.getTargetDurationSeconds()).isEqualTo(600);
        assertThat(target.getTargetSets()).isNull();
        assertThat(target.getTargetReps()).isNull();
    }

    @Test
    void activateProposalSetsAllSessionsAsIncomplete() {
        // Given a multi-session proposal
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedSession session1 = new ProposedSession(
                1, "Session 1",
                List.of(new ProposedExerciseTarget(
                        "Ex 1", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null
                ))
        );
        ProposedSession session2 = new ProposedSession(
                2, "Session 2",
                List.of(new ProposedExerciseTarget(
                        "Ex 2", ExerciseType.STRENGTH, 4, 8, null, null, null, null, null
                ))
        );
        ProposedSession session3 = new ProposedSession(
                3, "Session 3",
                List.of(new ProposedExerciseTarget(
                        "Ex 3", ExerciseType.CARDIO, null, null, null, null, 300, null, null
                ))
        );
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session1, session2, session3)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);

        // Then all sessions should start as incomplete
        assertThat(program.getSessions()).allMatch(s -> !s.isCompleted());
    }

    @Test
    void activateProposalGeneratesUniqueIds() {
        // Given two identical proposals for the same user
        var userId = UUID.randomUUID();
        ProposedSession session = new ProposedSession(
                1, "Workout",
                List.of(new ProposedExerciseTarget(
                        "Exercise", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null
                ))
        );
        PlanProposalResponse proposal1 = new PlanProposalResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );
        PlanProposalResponse proposal2 = new PlanProposalResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating both proposals separately
        WorkoutProgram program1 = activationService.activateProposal(userId, proposal1);
        WorkoutProgram program2 = activationService.activateProposal(userId, proposal2);

        // Then each should have unique IDs
        assertThat(program1.getId()).isNotEqualTo(program2.getId());
        assertThat(program1.getSessions().get(0).getId())
                .isNotEqualTo(program2.getSessions().get(0).getId());
    }

    @Test
    void activateProposalLinksSessionsAndExercises() {
        // Given a proposal
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedExerciseTarget ex = new ProposedExerciseTarget(
                "Exercise", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null
        );
        ProposedSession session = new ProposedSession(1, "Session", List.of(ex));
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);

        // Then sessions should be linked to program and exercises to sessions
        ProgramSession programSession = program.getSessions().get(0);
        assertThat(programSession.getProgram()).isEqualTo(program);

        ProgramExerciseTarget target = programSession.getExerciseTargets().get(0);
        assertThat(target.getProgramSession()).isEqualTo(programSession);
    }

    @Test
    void activateProposalPreservesExerciseOrder() {
        // Given a session with multiple exercises in order
        var userId = UUID.randomUUID();
        var proposalId = UUID.randomUUID();
        var attemptId = UUID.randomUUID();

        ProposedExerciseTarget ex1 = new ProposedExerciseTarget(
                "First", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null
        );
        ProposedExerciseTarget ex2 = new ProposedExerciseTarget(
                "Second", ExerciseType.STRENGTH, 4, 8, null, null, null, null, null
        );
        ProposedExerciseTarget ex3 = new ProposedExerciseTarget(
                "Third", ExerciseType.CARDIO, null, null, null, null, 300, null, null
        );
        ProposedSession session = new ProposedSession(1, "Workout", List.of(ex1, ex2, ex3));
        PlanProposalResponse proposal = new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                OnboardingEnums.ProposalStatus.PROPOSED,
                new GeneratedBy(OnboardingEnums.ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(session)
        );

        // When activating
        WorkoutProgram program = activationService.activateProposal(userId, proposal);
        List<ProgramExerciseTarget> targets = program.getSessions().get(0).getExerciseTargets();

        // Then exercise order should match and sortOrder should be set
        assertThat(targets).hasSize(3);
        assertThat(targets.get(0).getSortOrder()).isEqualTo(0);
        assertThat(targets.get(1).getSortOrder()).isEqualTo(1);
        assertThat(targets.get(2).getSortOrder()).isEqualTo(2);
    }
}

