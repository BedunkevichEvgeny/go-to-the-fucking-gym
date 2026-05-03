package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionValidatorTest {

    @Mock
    private ProgramExerciseTargetRepository programExerciseTargetRepository;

    private SessionValidatorService service;

    @BeforeEach
    void setUp() {
        service = new SessionValidatorService(programExerciseTargetRepository);
    }

    @Test
    void validateProgramSessionNotModifiableRejectsAddRemoveOrReorder() {
        UUID sessionId = UUID.randomUUID();
        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(sessionId)).thenReturn(List.of(
                ProgramExerciseTarget.builder().exerciseName("Bench Press").build(),
                ProgramExerciseTarget.builder().exerciseName("Pull Up").build()));

        List<ExerciseEntryInput> reordered = List.of(
                new ExerciseEntryInput(null, null, "Pull Up", ExerciseType.BODYWEIGHT, List.of(new StrengthSetInput(8, true, null, null)), List.of()),
                new ExerciseEntryInput(null, null, "Bench Press", ExerciseType.STRENGTH, List.of(new StrengthSetInput(8, false, new BigDecimal("70"), WeightUnit.KG)), List.of()));

        assertThatThrownBy(() -> service.validateProgramSessionNotModifiable(sessionId, reordered))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validateBodyweightSetAllowsNullWeightWhenBodyweight() {
        assertThatCode(() -> service.validateBodyweightSet(new StrengthSetInput(8, true, null, null))).doesNotThrowAnyException();
    }

    @Test
    void validateBodyweightSetAllowsNullWeightWhenNotBodyweight() {
        // weightValue is unconditionally optional per feature-001 spec; resistance bands and
        // cable machines may legitimately have no numeric weight.
        assertThatCode(() -> service.validateBodyweightSet(new StrengthSetInput(8, false, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSessionFeelingsRequiresRatingBetweenOneAndTen() {
        assertThatThrownBy(() -> service.validateSessionFeelings(new SessionFeelingsInput(11, "Too high")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.validateSessionFeelings(new SessionFeelingsInput(0, "Too low")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validateSessionFeelingsAllowsNullComment() {
        assertThatCode(() -> service.validateSessionFeelings(new SessionFeelingsInput(7, null))).doesNotThrowAnyException();
    }

    @Test
    void validateExerciseEntryRequiresSetsForStrengthAndLapsForCardio() {
        ExerciseEntryInput invalidStrength = new ExerciseEntryInput(
                null,
                null,
                "Bench Press",
                ExerciseType.STRENGTH,
                List.of(),
                List.of());
        ExerciseEntryInput invalidCardio = new ExerciseEntryInput(
                null,
                null,
                "Running",
                ExerciseType.CARDIO,
                List.of(),
                List.of());

        assertThatThrownBy(() -> service.validateExerciseEntry(invalidStrength))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one set");
        assertThatThrownBy(() -> service.validateExerciseEntry(invalidCardio))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one lap");
    }
}
