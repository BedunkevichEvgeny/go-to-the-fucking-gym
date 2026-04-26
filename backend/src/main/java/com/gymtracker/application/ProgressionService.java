package com.gymtracker.application;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.api.dto.ProgressionResponse;
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

    @Transactional(readOnly = true)
    public ProgressionResponse getExerciseProgression(UUID userId, String exerciseName) {
        List<ProgressionPoint> points = progressionQueryBuilder.fetchProgressionPoints(userId, exerciseName);
        return new ProgressionResponse(exerciseName, points);
    }
}

