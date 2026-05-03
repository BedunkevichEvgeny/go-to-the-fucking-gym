package com.gymtracker.api.dto;

import com.gymtracker.domain.SessionType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LoggedSessionDetail(
        UUID sessionId,
        SessionType sessionType,
        UUID programSessionId,
        LocalDate sessionDate,
        String name,
        String notes,
        Integer totalDurationSeconds,
        SessionFeelingsInput feelings,
        List<ExerciseEntryView> exerciseEntries,
        String aiSuggestion
) {
}

