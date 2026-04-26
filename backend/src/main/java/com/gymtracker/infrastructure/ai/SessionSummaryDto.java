package com.gymtracker.infrastructure.ai;

import com.gymtracker.domain.SessionType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SessionSummaryDto(
        UUID userId,
        UUID sessionId,
        SessionType sessionType,
        LocalDate sessionDate,
        Integer totalDurationSeconds,
        Integer feelingRating,
        String feelingComment,
        List<String> exercises
) {
}

