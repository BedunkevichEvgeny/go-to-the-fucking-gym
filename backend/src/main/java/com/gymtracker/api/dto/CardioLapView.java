package com.gymtracker.api.dto;

import com.gymtracker.domain.DistanceUnit;
import java.math.BigDecimal;

public record CardioLapView(
        int lapOrder,
        int durationSeconds,
        BigDecimal distanceValue,
        DistanceUnit distanceUnit
) {
}

