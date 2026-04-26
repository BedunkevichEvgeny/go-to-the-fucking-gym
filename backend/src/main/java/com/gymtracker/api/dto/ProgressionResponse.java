package com.gymtracker.api.dto;

import java.util.List;

public record ProgressionResponse(
        String exerciseName,
        List<ProgressionPoint> points
) {
}

