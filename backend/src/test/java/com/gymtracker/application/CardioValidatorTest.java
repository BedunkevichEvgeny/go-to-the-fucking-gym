package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardioValidatorTest {

    @Mock
    private ProgramExerciseTargetRepository programExerciseTargetRepository;

    @Test
    void validateCardioLapRequiresDurationAtLeastOneSecond() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);

        assertThatThrownBy(() -> service.validateCardioLap(new CardioLapInput(0, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 1 second");
    }

    @Test
    void validateCardioLapAllowsNullDistance() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);

        assertThatCode(() -> service.validateCardioLap(new CardioLapInput(300, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCardioEntryRequiresAtLeastOneLap() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);
        ExerciseEntryInput cardio = new ExerciseEntryInput(null, null, "Running", ExerciseType.CARDIO, List.of(), List.of());

        assertThatThrownBy(() -> service.validateExerciseEntry(cardio))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one lap");
    }

    @Test
    void cardioAndStrengthEntriesCanCoexistInSameSessionValidationFlow() {
        SessionValidatorService service = new SessionValidatorService(programExerciseTargetRepository);

        ExerciseEntryInput cardio = new ExerciseEntryInput(
                null,
                null,
                "Running",
                ExerciseType.CARDIO,
                List.of(),
                List.of(new CardioLapInput(420, null, null)));
        ExerciseEntryInput strength = new ExerciseEntryInput(
                null,
                null,
                "Bench Press",
                ExerciseType.STRENGTH,
                List.of(new StrengthSetInput(8, false, new BigDecimal("70"), com.gymtracker.domain.WeightUnit.KG)),
                List.of());

        assertThatCode(() -> {
            service.validateExerciseEntry(cardio);
            service.validateExerciseEntry(strength);
        }).doesNotThrowAnyException();
    }
}

