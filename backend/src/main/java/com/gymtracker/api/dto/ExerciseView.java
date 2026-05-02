package com.gymtracker.api.dto;

import com.gymtracker.domain.ExerciseType;
import java.util.UUID;

public record ExerciseView(
        UUID id,
        String name,
        String category,
        ExerciseType type,
        String description
) {
}

