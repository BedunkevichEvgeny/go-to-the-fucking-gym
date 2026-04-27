package com.gymtracker.infrastructure.ai;

import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SessionSummaryDTO(
        UUID userId,
        UUID sessionId,
        SessionType sessionType,
        LocalDate sessionDate,
        Integer totalDurationSeconds,
        FeelingsSummary feelings,
        UserPreferences metadata,
        List<ExerciseSummary> exercises
) {

    public String toPromptPayload() {
        return "sessionId=" + sessionId + ",sessionType=" + sessionType + ",sessionDate=" + sessionDate;
    }

    public record FeelingsSummary(
            Integer rating,
            String comment
    ) {
    }

    public record UserPreferences(
            String preferredWeightUnit
    ) {
    }

    public record ExerciseSummary(
            String exerciseName,
            ExerciseType exerciseType,
            ActualPerformance actual,
            TargetPerformance target
    ) {
    }

    public record ActualPerformance(
            Integer setCount,
            Integer totalReps,
            BigDecimal maxWeight,
            Integer totalDurationSeconds,
            BigDecimal totalDistance
    ) {
    }

    public record TargetPerformance(
            Integer targetSets,
            Integer targetReps,
            BigDecimal targetWeight,
            Integer targetDurationSeconds,
            BigDecimal targetDistance
    ) {
    }
}

