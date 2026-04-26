package com.gymtracker.api.dto;

import com.gymtracker.domain.ExerciseType;
import java.util.List;
import java.util.UUID;

public record ExerciseEntryView(
        UUID exerciseId,
        String customExerciseName,
        String exerciseName,
        ExerciseType exerciseType,
        List<StrengthSetView> sets,
        List<CardioLapView> cardioLaps
) {
}

