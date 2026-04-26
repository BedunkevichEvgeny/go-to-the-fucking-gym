package com.gymtracker.infrastructure.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.domain.CardioLap;
import com.gymtracker.domain.DistanceUnit;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.StrengthSet;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgressionQueryBuilderIT {

    @Autowired
    private ProgressionQueryBuilder progressionQueryBuilder;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Test
    void fetchProgressionPointsReturnsChronologicalStrengthPointsWithMaxWeight() {
        UUID userId = UUID.randomUUID();
        saveStrengthSession(userId, LocalDate.of(2026, 4, 27), "Deadlift", 110, 120, 115);
        saveStrengthSession(userId, LocalDate.of(2026, 4, 20), "deadlift", 100, 105);

        List<ProgressionPoint> points = progressionQueryBuilder.fetchProgressionPoints(userId, "DEADLIFT");

        assertThat(points).hasSize(2);
        assertThat(points.get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(points.get(0).metricType()).isEqualTo(ProgressionMetricType.WEIGHT);
        assertThat(points.get(0).metricValue()).isEqualTo(105.0);
        assertThat(points.get(1).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(points.get(1).metricValue()).isEqualTo(120.0);
    }

    @Test
    void fetchProgressionPointsPrefersDistanceAndFallsBackToDurationForCardio() {
        UUID userId = UUID.randomUUID();
        saveCardioSession(userId, LocalDate.of(2026, 4, 21), "Rowing", 420, null, 480, null);
        saveCardioSession(userId, LocalDate.of(2026, 4, 22), "Rowing", 300, "1.20", 600, "2.40");

        List<ProgressionPoint> points = progressionQueryBuilder.fetchProgressionPoints(userId, "rowing");

        assertThat(points).hasSize(2);
        assertThat(points.get(0).metricType()).isEqualTo(ProgressionMetricType.DURATION);
        assertThat(points.get(0).metricValue()).isEqualTo(900.0);
        assertThat(points.get(1).metricType()).isEqualTo(ProgressionMetricType.DISTANCE);
        assertThat(points.get(1).metricValue()).isEqualTo(3.6);
    }

    @Test
    void fetchProgressionPointsAppliesUserIsolationAndExerciseFilter() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        saveStrengthSession(userId, LocalDate.of(2026, 4, 21), "Bench Press", 80);
        saveStrengthSession(userId, LocalDate.of(2026, 4, 22), "Squat", 100);
        saveStrengthSession(otherUserId, LocalDate.of(2026, 4, 23), "Bench Press", 130);

        List<ProgressionPoint> points = progressionQueryBuilder.fetchProgressionPoints(userId, "bench");

        assertThat(points).hasSize(1);
        assertThat(points.getFirst().sessionDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(points.getFirst().metricValue()).isEqualTo(80.0);
    }

    private void saveStrengthSession(UUID userId, LocalDate date, String exerciseName, int... weights) {
        LoggedSession session = LoggedSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionType(SessionType.FREE)
                .sessionDate(date)
                .createdAt(OffsetDateTime.parse("2026-04-27T10:15:30Z"))
                .build();

        ExerciseEntry entry = ExerciseEntry.builder()
                .id(UUID.randomUUID())
                .loggedSession(session)
                .exerciseNameSnapshot(exerciseName)
                .exerciseType(ExerciseType.STRENGTH)
                .sortOrder(0)
                .build();

        List<StrengthSet> sets = java.util.stream.IntStream.range(0, weights.length)
                .mapToObj(index -> StrengthSet.builder()
                        .id(UUID.randomUUID())
                        .exerciseEntry(entry)
                        .setOrder(index + 1)
                        .reps(5)
                        .weightValue(BigDecimal.valueOf(weights[index]))
                        .weightUnit(WeightUnit.KG)
                        .bodyWeight(false)
                        .build())
                .toList();

        entry.setStrengthSets(sets);
        session.setExerciseEntries(List.of(entry));
        loggedSessionRepository.save(session);
    }

    private void saveCardioSession(
            UUID userId,
            LocalDate date,
            String exerciseName,
            int durationA,
            String distanceA,
            int durationB,
            String distanceB
    ) {
        LoggedSession session = LoggedSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionType(SessionType.FREE)
                .sessionDate(date)
                .createdAt(OffsetDateTime.parse("2026-04-27T10:15:30Z"))
                .build();

        ExerciseEntry entry = ExerciseEntry.builder()
                .id(UUID.randomUUID())
                .loggedSession(session)
                .exerciseNameSnapshot(exerciseName)
                .exerciseType(ExerciseType.CARDIO)
                .sortOrder(0)
                .build();

        List<CardioLap> laps = List.of(
                cardioLap(entry, 1, durationA, distanceA),
                cardioLap(entry, 2, durationB, distanceB));

        entry.setCardioLaps(laps);
        session.setExerciseEntries(List.of(entry));
        loggedSessionRepository.save(session);
    }

    private CardioLap cardioLap(ExerciseEntry entry, int lapOrder, int durationSeconds, String distanceValue) {
        return CardioLap.builder()
                .id(UUID.randomUUID())
                .exerciseEntry(entry)
                .lapOrder(lapOrder)
                .durationSeconds(durationSeconds)
                .distanceValue(distanceValue == null ? null : new BigDecimal(distanceValue))
                .distanceUnit(distanceValue == null ? null : DistanceUnit.KM)
                .build();
    }
}

