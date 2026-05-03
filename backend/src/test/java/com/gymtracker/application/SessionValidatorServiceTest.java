package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionValidatorServiceTest {

    @Mock
    private ProgramExerciseTargetRepository programExerciseTargetRepository;

    @Test
    void validateProgramSessionNotModifiableRejectsReorderedExercises() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);
        UUID sessionId = UUID.randomUUID();
        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(sessionId)).thenReturn(List.of(
                ProgramExerciseTarget.builder().exerciseName("Bench Press").build(),
                ProgramExerciseTarget.builder().exerciseName("Pull Up").build()));

        assertThatThrownBy(() -> service.validateProgramSessionNotModifiable(sessionId, List.of(
                new ExerciseEntryInput(null, null, "Pull Up", ExerciseType.BODYWEIGHT, List.of(new StrengthSetInput(8, true, null, null)), List.of()),
                new ExerciseEntryInput(null, null, "Bench Press", ExerciseType.STRENGTH, List.of(new StrengthSetInput(8, false, new BigDecimal("70"), com.gymtracker.domain.WeightUnit.KG)), List.of())
        ))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validateBodyweightSetRejectsWeightForBodyweight() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);

        assertThatThrownBy(() -> service.validateBodyweightSet(new StrengthSetInput(8, true, new BigDecimal("20"), com.gymtracker.domain.WeightUnit.KG)))
                .isInstanceOf(ValidationException.class);
    }

    // ── T076-BUG-009-TEST: regression tests for validateBodyweightSet edge cases ──────────────

    @Test
    void validateBodyweightSet_NonBodyweightWithNullWeight_DoesNotThrow() {
        // { isBodyWeight: false, weightValue: null } must NOT throw
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);
        assertThatCode(() -> service.validateBodyweightSet(new StrengthSetInput(8, false, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateBodyweightSet_BodyweightWithNonNullWeight_Throws() {
        // { isBodyWeight: true, weightValue: 50.0 } DOES throw
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);
        assertThatThrownBy(() -> service.validateBodyweightSet(
                new StrengthSetInput(8, true, new BigDecimal("50.0"), com.gymtracker.domain.WeightUnit.KG)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validateBodyweightSet_NonBodyweightWithWeight_DoesNotThrow() {
        // { isBodyWeight: false, weightValue: 7.5 } does NOT throw
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);
        assertThatCode(() -> service.validateBodyweightSet(
                new StrengthSetInput(8, false, new BigDecimal("7.5"), com.gymtracker.domain.WeightUnit.KG)))
                .doesNotThrowAnyException();
    }
}

