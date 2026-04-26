package com.gymtracker.api.dto;

import com.gymtracker.domain.SessionType;
import java.time.LocalDate;
import java.util.UUID;

public record SessionHistoryItem(
        UUID sessionId,
        LocalDate sessionDate,
        SessionType sessionType,
        int exerciseCount,
        Integer totalDurationSeconds,
        String name
) {
}

