package com.gymtracker.api.dto;

import java.util.List;
import java.util.UUID;

public record ProgramSessionView(
        UUID programSessionId,
        int sequenceNumber,
        String name,
        List<ProgramExerciseTargetView> exercises
) {
}

