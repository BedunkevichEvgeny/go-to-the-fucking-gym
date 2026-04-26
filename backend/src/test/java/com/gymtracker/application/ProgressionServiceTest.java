package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.domain.CardioLap;
import com.gymtracker.domain.DistanceUnit;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.StrengthSet;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProgressionServiceTest {

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    private ProgressionService service;

    @BeforeEach
    void setUp() {
        service = new ProgressionService(loggedSessionRepository, new DtoMapper());
    }

    @Test
    void getExerciseProgressionReturnsChronologicalPointsWithExpectedFields() {
        UUID userId = UUID.randomUUID();
        LoggedSession newer = strengthSession(userId, SessionType.FREE, LocalDate.of(2026, 4, 27), "Deadlift", 120, 125);
        LoggedSession older = strengthSession(userId, SessionType.PROGRAM, LocalDate.of(2026, 4, 20), "Deadlift", 110, 115);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newer, older)));

        ProgressionResponse result = service.getExerciseProgression(userId, "Deadlift");

        assertThat(result.exerciseName()).isEqualTo("Deadlift");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(result.points().get(1).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(result.points())
                .allSatisfy(point -> {
                    assertThat(point.sessionId()).isNotNull();
                    assertThat(point.metricType()).isEqualTo(ProgressionMetricType.WEIGHT);
                    assertThat(point.metricValue()).isPositive();
                });
    }

    @Test
    void getExerciseProgressionAggregatesProgramAndFreeSessions() {
        UUID userId = UUID.randomUUID();
        LoggedSession freeSession = strengthSession(userId, SessionType.FREE, LocalDate.of(2026, 4, 22), "Bench Press", 70);
        LoggedSession programSession = strengthSession(userId, SessionType.PROGRAM, LocalDate.of(2026, 4, 24), "Bench Press", 75);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(programSession, freeSession)));

        ProgressionResponse result = service.getExerciseProgression(userId, "Bench Press");

        assertThat(result.points()).hasSize(2);
    }

    @Test
    void getExerciseProgressionReturnsEmptyListWhenExerciseWasNeverLogged() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = strengthSession(userId, SessionType.FREE, LocalDate.of(2026, 4, 27), "Squat", 100);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));

        ProgressionResponse result = service.getExerciseProgression(userId, "Deadlift");

        assertThat(result.points()).isEmpty();
    }

    @Test
    void getExerciseProgressionReturnsSinglePointWhenOnlyOneSessionExists() {
        UUID userId = UUID.randomUUID();
        LoggedSession single = strengthSession(userId, SessionType.FREE, LocalDate.of(2026, 4, 27), "Overhead Press", 55);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(single)));

        ProgressionResponse result = service.getExerciseProgression(userId, "Overhead Press");

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().metricType()).isEqualTo(ProgressionMetricType.WEIGHT);
    }

    @Test
    void getExerciseProgressionUsesMaxWeightForStrengthMetricValue() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = strengthSession(userId, SessionType.FREE, LocalDate.of(2026, 4, 27), "Deadlift", 100, 115, 110);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));

        ProgressionResponse result = service.getExerciseProgression(userId, "Deadlift");

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().metricType()).isEqualTo(ProgressionMetricType.WEIGHT);
        assertThat(result.points().getFirst().metricValue()).isEqualTo(115.0);
    }

    @Test
    void getExerciseProgressionUsesDistanceForCardioWhenDistanceExists() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = cardioSession(userId, LocalDate.of(2026, 4, 27), "Running", 300, "1.2", 600, "2.4");

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));

        ProgressionResponse result = service.getExerciseProgression(userId, "running");

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().metricType()).isEqualTo(ProgressionMetricType.DISTANCE);
        assertThat(result.points().getFirst().metricValue()).isEqualTo(3.6);
    }

    @Test
    void getExerciseProgressionFallsBackToDurationForCardioWithoutDistance() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = cardioSession(userId, LocalDate.of(2026, 4, 27), "Rowing", 420, null, 480, null);

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));

        ProgressionResponse result = service.getExerciseProgression(userId, "ROWING");

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().metricType()).isEqualTo(ProgressionMetricType.DURATION);
        assertThat(result.points().getFirst().metricValue()).isEqualTo(900.0);
    }

    private static LoggedSession strengthSession(UUID userId, SessionType sessionType, LocalDate date, String exerciseName, int... weights) {
        LoggedSession session = LoggedSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionType(sessionType)
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
        return session;
    }

    private static LoggedSession cardioSession(
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
                CardioLap.builder()
                        .id(UUID.randomUUID())
                        .exerciseEntry(entry)
                        .lapOrder(1)
                        .durationSeconds(durationA)
                        .distanceValue(distanceA == null ? null : new BigDecimal(distanceA))
                        .distanceUnit(distanceA == null ? null : DistanceUnit.KM)
                        .build(),
                CardioLap.builder()
                        .id(UUID.randomUUID())
                        .exerciseEntry(entry)
                        .lapOrder(2)
                        .durationSeconds(durationB)
                        .distanceValue(distanceB == null ? null : new BigDecimal(distanceB))
                        .distanceUnit(distanceB == null ? null : DistanceUnit.KM)
                        .build());
        entry.setCardioLaps(laps);
        session.setExerciseEntries(List.of(entry));
        return session;
    }
}

