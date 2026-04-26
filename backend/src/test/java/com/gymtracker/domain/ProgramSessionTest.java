package com.gymtracker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgramSessionTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createsProgramSessionWithValidFields() {
        ProgramSession session = ProgramSession.builder()
                .id(UUID.randomUUID())
                .sequenceNumber(1)
                .name("Upper Body")
                .completed(false)
                .build();

        assertThat(session.getSequenceNumber()).isEqualTo(1);
        assertThat(session.getName()).isEqualTo("Upper Body");
        assertThat(session.isCompleted()).isFalse();
    }

    @Test
    void keepsExerciseTargetsRelationship() {
        ProgramExerciseTarget target = ProgramExerciseTarget.builder()
                .id(UUID.randomUUID())
                .exerciseName("Bench Press")
                .exerciseType(ExerciseType.STRENGTH)
                .targetSets(3)
                .targetReps(8)
                .sortOrder(0)
                .build();
        ProgramSession session = ProgramSession.builder()
                .id(UUID.randomUUID())
                .sequenceNumber(1)
                .name("Upper Body")
                .exerciseTargets(List.of(target))
                .build();

        assertThat(session.getExerciseTargets()).containsExactly(target);
    }

    @Test
    void validatesSequenceNumberAndNameConstraints() {
        ProgramSession invalid = ProgramSession.builder()
                .id(UUID.randomUUID())
                .sequenceNumber(0)
                .name("")
                .build();

        assertThat(validator.validate(invalid))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sequenceNumber", "name");
    }

    @Test
    void supportsStateTransitionFromIncompleteToComplete() {
        ProgramSession session = ProgramSession.builder()
                .id(UUID.randomUUID())
                .sequenceNumber(1)
                .name("Upper Body")
                .completed(false)
                .build();

        session.setCompleted(true);

        assertThat(session.isCompleted()).isTrue();
    }
}
