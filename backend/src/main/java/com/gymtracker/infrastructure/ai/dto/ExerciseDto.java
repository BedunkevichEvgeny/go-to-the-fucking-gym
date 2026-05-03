package com.gymtracker.infrastructure.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;

/**
 * Structured output DTO representing a single exercise in an AI-generated onboarding plan.
 *
 * <p>The {@link ExerciseType} field uses a safe deserializer so that unknown values
 * returned by the LLM (e.g., {@code "FLEXIBILITY"}) fall back to {@code STRENGTH}
 * rather than throwing {@link IllegalArgumentException}.
 */
public record ExerciseDto(
        String name,
        @JsonDeserialize(using = SafeExerciseTypeDeserializer.class)
        ExerciseType type,
        Integer targetSets,
        Integer targetReps,
        BigDecimal targetWeight,
        WeightUnit weightUnit,
        Integer durationSeconds
) {}

