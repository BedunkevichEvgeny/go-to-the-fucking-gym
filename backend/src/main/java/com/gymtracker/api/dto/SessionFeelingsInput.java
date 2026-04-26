package com.gymtracker.api.dto;

import com.gymtracker.infrastructure.validation.ValidRating;

public record SessionFeelingsInput(
        @ValidRating Integer rating,
        String comment
) {
}

