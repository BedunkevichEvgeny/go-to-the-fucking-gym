package com.gymtracker.infrastructure.config;

import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.User;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import com.gymtracker.infrastructure.repository.UserRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final SecurityUsersProperties securityUsersProperties;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutProgramRepository workoutProgramRepository;

    public DataSeeder(
            SecurityUsersProperties securityUsersProperties,
            UserRepository userRepository,
            ExerciseRepository exerciseRepository,
            WorkoutProgramRepository workoutProgramRepository
    ) {
        this.securityUsersProperties = securityUsersProperties;
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.workoutProgramRepository = workoutProgramRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, SecurityUsersProperties.UserDefinition> entry : securityUsersProperties.definitions().entrySet()) {
            SecurityUsersProperties.UserDefinition definition = entry.getValue();
            userRepository.findById(definition.getId()).orElseGet(() -> userRepository.save(User.builder()
                    .id(definition.getId())
                    .preferredWeightUnit(definition.getPreferredWeightUnit())
                    .build()));
        }

        if (exerciseRepository.count() == 0) {
            exerciseRepository.saveAll(List.of(
                    Exercise.builder().name("Bench Press").category("Chest").type(ExerciseType.STRENGTH).description("Barbell bench press").active(true).build(),
                    Exercise.builder().name("Back Squat").category("Legs").type(ExerciseType.STRENGTH).description("Barbell squat").active(true).build(),
                    Exercise.builder().name("Deadlift").category("Posterior Chain").type(ExerciseType.STRENGTH).description("Conventional deadlift").active(true).build(),
                    Exercise.builder().name("Pull Up").category("Back").type(ExerciseType.BODYWEIGHT).description("Strict pull up").active(true).build(),
                    Exercise.builder().name("Running").category("Cardio").type(ExerciseType.CARDIO).description("Steady state run").active(true).build()
            ));
        }

        SecurityUsersProperties.UserDefinition user1 = securityUsersProperties.definitions().get("user1");
        if (user1 != null && workoutProgramRepository.findFirstByUserIdAndStatus(user1.getId(), ProgramStatus.ACTIVE).isEmpty()) {
            WorkoutProgram program = WorkoutProgram.builder()
                    .userId(user1.getId())
                    .name("Starter Strength Cycle")
                    .status(ProgramStatus.ACTIVE)
                    .build();

            ProgramSession session1 = ProgramSession.builder().program(program).sequenceNumber(1).name("Upper Body").completed(false).build();
            ProgramSession session2 = ProgramSession.builder().program(program).sequenceNumber(2).name("Lower Body").completed(false).build();
            program.setSessions(List.of(session1, session2));

            session1.setExerciseTargets(List.of(
                    ProgramExerciseTarget.builder().programSession(session1).exerciseName("Bench Press").exerciseType(ExerciseType.STRENGTH).targetSets(3).targetReps(8).targetWeight(new BigDecimal("70.0")).targetWeightUnit(com.gymtracker.domain.WeightUnit.KG).sortOrder(0).build(),
                    ProgramExerciseTarget.builder().programSession(session1).exerciseName("Pull Up").exerciseType(ExerciseType.BODYWEIGHT).targetSets(3).targetReps(8).sortOrder(1).build(),
                    ProgramExerciseTarget.builder().programSession(session1).exerciseName("Running").exerciseType(ExerciseType.CARDIO).targetDurationSeconds(900).sortOrder(2).build()
            ));
            session2.setExerciseTargets(List.of(
                    ProgramExerciseTarget.builder().programSession(session2).exerciseName("Back Squat").exerciseType(ExerciseType.STRENGTH).targetSets(4).targetReps(6).targetWeight(new BigDecimal("100.0")).targetWeightUnit(com.gymtracker.domain.WeightUnit.KG).sortOrder(0).build(),
                    ProgramExerciseTarget.builder().programSession(session2).exerciseName("Deadlift").exerciseType(ExerciseType.STRENGTH).targetSets(1).targetReps(5).targetWeight(new BigDecimal("120.0")).targetWeightUnit(com.gymtracker.domain.WeightUnit.KG).sortOrder(1).build()
            ));
            workoutProgramRepository.save(program);
        }
    }
}


