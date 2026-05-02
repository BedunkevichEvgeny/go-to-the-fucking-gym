package com.gymtracker.application;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.infrastructure.query.ProgressionQueryBuilder;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressionService {

    private final ProgressionQueryBuilder progressionQueryBuilder;

    public ProgressionService(ProgressionQueryBuilder progressionQueryBuilder) {
        this.progressionQueryBuilder = progressionQueryBuilder;
    }

    /**
     * Fetches progression points for a specific exercise and user.
     *
     * @param userId authenticated user identifier
     * @param exerciseName exercise display name used for matching historical entries
     * @return progression response with chronological points for charting
     * @throws ValidationException when exerciseName is null or blank
     */
    @Transactional(readOnly = true)
    public ProgressionResponse getExerciseProgression(UUID userId, String exerciseName) {
        if (exerciseName == null || exerciseName.isBlank()) {
            throw new ValidationException("Exercise name is required for progression");
        }
        String normalizedExerciseName = exerciseName.trim();
        List<ProgressionPoint> points = progressionQueryBuilder.fetchProgressionPoints(
                userId,
                normalizedExerciseName);
        return new ProgressionResponse(normalizedExerciseName, points);
    }
}

