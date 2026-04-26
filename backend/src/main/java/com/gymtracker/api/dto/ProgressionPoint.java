package com.gymtracker.api.dto;

import com.gymtracker.domain.ProgressionMetricType;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressionPoint(
        UUID sessionId,
        LocalDate sessionDate,
        ProgressionMetricType metricType,
        double metricValue
) {
}

