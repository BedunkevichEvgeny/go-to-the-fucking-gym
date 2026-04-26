package com.gymtracker.api.dto;

import com.gymtracker.domain.DistanceUnit;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record CardioLapInput(
        @Min(1) Integer durationSeconds,
        BigDecimal distanceValue,
        DistanceUnit distanceUnit
) {
}

