package com.gymtracker.api.dto;

import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;

public record StrengthSetView(
        int setOrder,
        int reps,
        BigDecimal weightValue,
        WeightUnit weightUnit,
        boolean isBodyWeight,
        Integer durationSeconds,
        Integer restSeconds
) {
}

