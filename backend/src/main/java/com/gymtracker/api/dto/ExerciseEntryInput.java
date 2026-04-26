package com.gymtracker.api.dto;

import com.gymtracker.domain.ExerciseType;
import com.gymtracker.infrastructure.validation.ValidExerciseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ExerciseEntryInput(
        UUID exerciseId,
        String customExerciseName,
        @NotBlank String exerciseName,
        @ValidExerciseType ExerciseType exerciseType,
        @Valid List<StrengthSetInput> sets,
        @Valid List<CardioLapInput> cardioLaps
) {
}

