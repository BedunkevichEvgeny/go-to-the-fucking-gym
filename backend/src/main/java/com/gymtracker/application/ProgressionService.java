package com.gymtracker.application;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.SessionSpecifications;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressionService {

    private final LoggedSessionRepository loggedSessionRepository;
    private final DtoMapper dtoMapper;

    public ProgressionService(LoggedSessionRepository loggedSessionRepository, DtoMapper dtoMapper) {
        this.loggedSessionRepository = loggedSessionRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public ProgressionResponse getExerciseProgression(UUID userId, String exerciseName) {
        String normalized = exerciseName.toLowerCase(Locale.ROOT);
        var sessions = loggedSessionRepository.findAll(SessionSpecifications.forUser(userId), PageRequest.of(0, 500)).getContent();
        List<ProgressionPoint> points = new ArrayList<>();
        sessions.forEach(session -> session.getExerciseEntries().stream()
                .filter(entry -> entry.getExerciseNameSnapshot().toLowerCase(Locale.ROOT).contains(normalized))
                .findFirst()
                .ifPresent(entry -> points.add(toPoint(session.getId(), session.getSessionDate(), entry))));
        points.sort(Comparator.comparing(ProgressionPoint::sessionDate));
        return new ProgressionResponse(exerciseName, points);
    }

    private ProgressionPoint toPoint(UUID sessionId, java.time.LocalDate sessionDate, ExerciseEntry entry) {
        if (!entry.getStrengthSets().isEmpty()) {
            double maxWeight = entry.getStrengthSets().stream()
                    .filter(set -> set.getWeightValue() != null)
                    .mapToDouble(set -> set.getWeightValue().doubleValue())
                    .max()
                    .orElse(entry.getStrengthSets().stream().mapToDouble(set -> set.getReps()).max().orElse(0));
            return dtoMapper.toProgressionPoint(sessionId, sessionDate, ProgressionMetricType.WEIGHT, maxWeight);
        }
        double totalDistance = entry.getCardioLaps().stream()
                .filter(lap -> lap.getDistanceValue() != null)
                .mapToDouble(lap -> lap.getDistanceValue().doubleValue())
                .sum();
        if (totalDistance > 0) {
            return dtoMapper.toProgressionPoint(sessionId, sessionDate, ProgressionMetricType.DISTANCE, totalDistance);
        }
        double totalDuration = entry.getCardioLaps().stream().mapToDouble(lap -> lap.getDurationSeconds()).sum();
        return dtoMapper.toProgressionPoint(sessionId, sessionDate, ProgressionMetricType.DURATION, totalDuration);
    }
}

