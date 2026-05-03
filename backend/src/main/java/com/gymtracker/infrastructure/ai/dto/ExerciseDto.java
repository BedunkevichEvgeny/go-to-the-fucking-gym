package com.gymtracker.infrastructure.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.WeightUnit;
import dev.langchain4j.model.output.structured.Description;
import java.math.BigDecimal;

/**
 * Structured output DTO representing a single exercise in an AI-generated onboarding plan.
 *
 * <p>The {@link ExerciseType} field uses a safe deserializer so that unknown values
 * returned by the LLM (e.g., {@code "FLEXIBILITY"}) fall back to {@code STRENGTH}
 * rather than throwing {@link IllegalArgumentException}.
 */
@Description("A single exercise within a workout session")
public record ExerciseDto(
        @Description("Full name of the exercise, e.g., 'Dumbbell Bench Press' or 'Bodyweight Squat'")
        String name,

        @Description("Exercise category. Must be exactly one of: STRENGTH, BODYWEIGHT, CARDIO")
        @JsonDeserialize(using = SafeExerciseTypeDeserializer.class)
        ExerciseType type,

        @Description("Number of sets to perform. Null for cardio/timed exercises.")
        Integer targetSets,

        @Description("Number of repetitions per set. Null for cardio/timed exercises.")
        Integer targetReps,

        @Description("Target weight for the exercise. Null for bodyweight or cardio exercises.")
        BigDecimal targetWeight,

        @Description("Unit for targetWeight. Use KG or LB. Null if targetWeight is null.")
        WeightUnit weightUnit,

        @Description("Duration of the exercise in seconds. Null for set/rep-based exercises.")
        Integer durationSeconds
) {}
