package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.infrastructure.query.ProgressionQueryBuilder;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressionServiceTest {

    @Mock
    private ProgressionQueryBuilder progressionQueryBuilder;

    private ProgressionService service;

    @BeforeEach
    void setUp() {
        service = new ProgressionService(progressionQueryBuilder);
    }

    @Test
    void getExerciseProgressionDelegatesToQueryBuilderAndReturnsPoints() {
        UUID userId = UUID.randomUUID();
        List<ProgressionPoint> points = List.of(
                new ProgressionPoint(UUID.randomUUID(), LocalDate.of(2026, 4, 20), ProgressionMetricType.WEIGHT, 115.0),
                new ProgressionPoint(UUID.randomUUID(), LocalDate.of(2026, 4, 27), ProgressionMetricType.WEIGHT, 125.0));

        when(progressionQueryBuilder.fetchProgressionPoints(userId, "Deadlift")).thenReturn(points);

        ProgressionResponse result = service.getExerciseProgression(userId, "Deadlift");

        verify(progressionQueryBuilder).fetchProgressionPoints(eq(userId), eq("Deadlift"));
        assertThat(result.exerciseName()).isEqualTo("Deadlift");
        assertThat(result.points()).containsExactlyElementsOf(points);
    }

    @Test
    void getExerciseProgressionPreservesRequestedExerciseNameForEmptyResults() {
        UUID userId = UUID.randomUUID();
        when(progressionQueryBuilder.fetchProgressionPoints(userId, "Unknown Exercise")).thenReturn(List.of());

        ProgressionResponse result = service.getExerciseProgression(userId, "Unknown Exercise");

        assertThat(result.exerciseName()).isEqualTo("Unknown Exercise");
        assertThat(result.points()).isEmpty();
    }
}


