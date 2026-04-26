package com.gymtracker.api.dto;

import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.validation.ValidWeightUnit;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record StrengthSetInput(
        @Min(1) Integer reps,
        Boolean isBodyWeight,
        BigDecimal weightValue,
        @ValidWeightUnit WeightUnit weightUnit
) {
}

