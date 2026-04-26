package com.gymtracker.api.dto;

import com.gymtracker.domain.DistanceUnit;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;

public record ProgramExerciseTargetView(
        String exerciseName,
        ExerciseType exerciseType,
        Integer targetSets,
        Integer targetReps,
        BigDecimal targetWeight,
        WeightUnit targetWeightUnit,
        Integer targetDurationSeconds,
        BigDecimal targetDistance,
        DistanceUnit targetDistanceUnit
) {
}

