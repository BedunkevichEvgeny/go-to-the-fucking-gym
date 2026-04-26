package com.gymtracker.api.dto;

import java.time.OffsetDateTime;

public record ApiError(
        String code,
        String message,
        OffsetDateTime timestamp
) {
}

