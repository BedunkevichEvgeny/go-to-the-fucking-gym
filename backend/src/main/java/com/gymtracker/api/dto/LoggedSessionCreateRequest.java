package com.gymtracker.api.dto;

import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.validation.ValidSessionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LoggedSessionCreateRequest(
        @ValidSessionType SessionType sessionType,
        UUID programSessionId,
        @NotNull LocalDate sessionDate,
        String name,
        String notes,
        @Min(1) Integer totalDurationSeconds,
        @Valid @NotNull SessionFeelingsInput feelings,
        @Valid @NotEmpty List<ExerciseEntryInput> exerciseEntries
) {
}

