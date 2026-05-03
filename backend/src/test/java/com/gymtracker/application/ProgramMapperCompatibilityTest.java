package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedExerciseTarget;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * T039: Unit tests for mapping proposal sessions to 001 workout program entities.
 *
 * Verifies that proposed sessions and exercises can be correctly transformed into
 * WorkoutProgram, ProgramSession, and ProgramExerciseTarget entities that are
 * compatible with existing 001 contracts and behavior.
 */
class ProgramMapperCompatibilityTest {

    private ProgramMapperCompatibility mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProgramMapperCompatibility();
    }

    @Test
    void mapProposedSessionsToProgramCreatesValidProgram() {
        // Given a list of proposed sessions
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
        ProposedSession session = new ProposedSession(
                1,
                "Upper Body",
                List.of(benchPress)
        );

        var userId = java.util.UUID.randomUUID();

        // When mapping to program entities
        WorkoutProgram program = mapper.mapToProgram(userId, "Generated Plan", List.of(session));

        // Then the program should have correct structure
        assertThat(program).isNotNull();
        assertThat(program.getId()).isNotNull();
        assertThat(program.getUserId()).isEqualTo(userId);
        assertThat(program.getName()).isEqualTo("Generated Plan");
        assertThat(program.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(program.getSessions()).hasSize(1);
    }

    @Test
    void mapProposedSessionTosProgramSessionPreservesSequenceAndExercises() {
        // Given proposed sessions with specific sequence and exercises
        ProposedExerciseTarget dumbellCurl = new ProposedExerciseTarget(
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
        ProposedExerciseTarget tricepDips = new ProposedExerciseTarget(
                "Tricep Dips",
                ExerciseType.STRENGTH,
                3,
                12,
                null,
                null,
                null,
                null,
                null
        );
        ProposedSession session = new ProposedSession(
                2,
                "Arms",
                List.of(dumbellCurl, tricepDips)
        );

        var userId = java.util.UUID.randomUUID();

        // When mapping
        WorkoutProgram program = mapper.mapToProgram(userId, "Arms Workout", List.of(session));

        // Then program sessions should preserve structure
        ProgramSession programSession = program.getSessions().get(0);
        assertThat(programSession).isNotNull();
        assertThat(programSession.getSequenceNumber()).isEqualTo(2);
        assertThat(programSession.getName()).isEqualTo("Arms");
        assertThat(programSession.isCompleted()).isFalse();
        assertThat(programSession.getExerciseTargets()).hasSize(2);
    }

    @Test
    void mapExerciseTargetMaintainsExerciseMetadataAndType() {
        // Given proposed exercise with strength metrics
        ProposedExerciseTarget squats = new ProposedExerciseTarget(
                "Back Squat",
                ExerciseType.STRENGTH,
                5,
                5,
                BigDecimal.valueOf(120),
                WeightUnit.KG,
                null,
                null,
                null
        );
        ProposedSession session = new ProposedSession(1, "Legs", List.of(squats));

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Leg Day", List.of(session));
        ProgramExerciseTarget target = program.getSessions().get(0).getExerciseTargets().get(0);

        // Then exercise target should preserve all metrics
        assertThat(target).isNotNull();
        assertThat(target.getExerciseName()).isEqualTo("Back Squat");
        assertThat(target.getExerciseType()).isEqualTo(ExerciseType.STRENGTH);
        assertThat(target.getTargetSets()).isEqualTo(5);
        assertThat(target.getTargetReps()).isEqualTo(5);
        assertThat(target.getTargetWeight()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(target.getTargetWeightUnit()).isEqualTo(WeightUnit.KG);
    }

    @Test
    void mapExerciseTargetHandlesCardioMetrics() {
        // Given proposed exercise with cardio duration metrics
        ProposedExerciseTarget treadmill = new ProposedExerciseTarget(
                "Treadmill Run",
                ExerciseType.CARDIO,
                null,
                null,
                null,
                null,
                600, // 10 minutes in seconds
                null,
                null
        );
        ProposedSession session = new ProposedSession(1, "Cardio", List.of(treadmill));

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Cardio Day", List.of(session));
        ProgramExerciseTarget target = program.getSessions().get(0).getExerciseTargets().get(0);

        // Then cardio metrics should be preserved
        assertThat(target).isNotNull();
        assertThat(target.getExerciseType()).isEqualTo(ExerciseType.CARDIO);
        assertThat(target.getTargetDurationSeconds()).isEqualTo(600);
        assertThat(target.getTargetSets()).isNull();
        assertThat(target.getTargetReps()).isNull();
    }

    @Test
    void mapExerciseTargetHandlesDistanceMetrics() {
        // Given proposed exercise with distance metrics
        ProposedExerciseTarget biking = new ProposedExerciseTarget(
                "Outdoor Cycling",
                ExerciseType.CARDIO,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(10),
                DistanceUnit.KM
        );
        ProposedSession session = new ProposedSession(1, "Cycling", List.of(biking));

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Bike Ride", List.of(session));
        ProgramExerciseTarget target = program.getSessions().get(0).getExerciseTargets().get(0);

        // Then distance metrics should be preserved
        assertThat(target).isNotNull();
        assertThat(target.getTargetDistance()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(target.getTargetDistanceUnit()).isEqualTo(DistanceUnit.KM);
        assertThat(target.getTargetDurationSeconds()).isNull();
    }

    @Test
    void mapMultipleSessionsPreservesOrder() {
        // Given multiple proposed sessions in order
        ProposedSession session1 = new ProposedSession(
                1,
                "Day 1",
                List.of(new ProposedExerciseTarget("Exercise 1", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null))
        );
        ProposedSession session2 = new ProposedSession(
                2,
                "Day 2",
                List.of(new ProposedExerciseTarget("Exercise 2", ExerciseType.CARDIO, null, null, null, null, 300, null, null))
        );
        ProposedSession session3 = new ProposedSession(
                3,
                "Day 3",
                List.of(new ProposedExerciseTarget("Exercise 3", ExerciseType.STRENGTH, 4, 8, null, null, null, null, null))
        );

        // When mapping all sessions
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "3-Day Split", List.of(session1, session2, session3));

        // Then all sessions should be present and ordered
        assertThat(program.getSessions()).hasSize(3);
        assertThat(program.getSessions().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(program.getSessions().get(1).getSequenceNumber()).isEqualTo(2);
        assertThat(program.getSessions().get(2).getSequenceNumber()).isEqualTo(3);
    }

    @Test
    void mapSessionWithMultipleExercisesPreservesExerciseOrder() {
        // Given a session with multiple exercises
        ProposedExerciseTarget ex1 = new ProposedExerciseTarget(
                "Exercise 1", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null
        );
        ProposedExerciseTarget ex2 = new ProposedExerciseTarget(
                "Exercise 2", ExerciseType.STRENGTH, 4, 8, null, null, null, null, null
        );
        ProposedExerciseTarget ex3 = new ProposedExerciseTarget(
                "Exercise 3", ExerciseType.CARDIO, null, null, null, null, 300, null, null
        );
        ProposedSession session = new ProposedSession(1, "Full Body", List.of(ex1, ex2, ex3));

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Full Body Day", List.of(session));
        List<ProgramExerciseTarget> targets = program.getSessions().get(0).getExerciseTargets();

        // Then exercises should be ordered and preserving names
        assertThat(targets).hasSize(3);
        assertThat(targets.get(0).getExerciseName()).isEqualTo("Exercise 1");
        assertThat(targets.get(1).getExerciseName()).isEqualTo("Exercise 2");
        assertThat(targets.get(2).getExerciseName()).isEqualTo("Exercise 3");
    }

    @Test
    void mapProgramSetsCorrectInitialStatus() {
        // Given any proposed sessions
        ProposedSession session = new ProposedSession(
                1,
                "Test",
                List.of(new ProposedExerciseTarget("Test Ex", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null))
        );

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Test Program", List.of(session));

        // Then program should be in ACTIVE status (ready to use in 001 tracker)
        assertThat(program.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(program.getCompletedAt()).isNull();
    }

    @Test
    void mapProgramSessionsAreIncompleteByDefault() {
        // Given any proposed sessions
        ProposedSession session1 = new ProposedSession(
                1,
                "Session 1",
                List.of(new ProposedExerciseTarget("Ex 1", ExerciseType.STRENGTH, 3, 10, null, null, null, null, null))
        );
        ProposedSession session2 = new ProposedSession(
                2,
                "Session 2",
                List.of(new ProposedExerciseTarget("Ex 2", ExerciseType.STRENGTH, 4, 8, null, null, null, null, null))
        );

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Program", List.of(session1, session2));

        // Then all sessions should start as incomplete
        assertThat(program.getSessions()).allMatch(s -> !s.isCompleted());
    }

    @Test
    void mapExerciseTargetsSortOrderMatchesListIndex() {
        // Given exercises in specific order
        ProposedExerciseTarget ex1 = new ProposedExerciseTarget(
                "First", ExerciseType.STRENGTH, 1, 1, null, null, null, null, null
        );
        ProposedExerciseTarget ex2 = new ProposedExerciseTarget(
                "Second", ExerciseType.STRENGTH, 2, 2, null, null, null, null, null
        );
        ProposedExerciseTarget ex3 = new ProposedExerciseTarget(
                "Third", ExerciseType.STRENGTH, 3, 3, null, null, null, null, null
        );
        ProposedSession session = new ProposedSession(1, "Ordered", List.of(ex1, ex2, ex3));

        // When mapping
        var userId = java.util.UUID.randomUUID();
        WorkoutProgram program = mapper.mapToProgram(userId, "Program", List.of(session));
        List<ProgramExerciseTarget> targets = program.getSessions().get(0).getExerciseTargets();

        // Then each exercise should have sortOrder matching its position
        assertThat(targets.get(0).getSortOrder()).isEqualTo(0);
        assertThat(targets.get(1).getSortOrder()).isEqualTo(1);
        assertThat(targets.get(2).getSortOrder()).isEqualTo(2);
    }
}

