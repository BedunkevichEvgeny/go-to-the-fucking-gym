package com.gymtracker.infrastructure.ai.dto;

import java.util.List;

/**
 * Structured output DTO for a single workout session inside an AI-generated onboarding plan.
 */
public record SessionDto(
        int sequenceNumber,
        String name,
        List<ExerciseDto> exercises
) {}

