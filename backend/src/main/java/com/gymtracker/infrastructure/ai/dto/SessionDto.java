package com.gymtracker.infrastructure.ai.dto;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;

/**
 * Structured output DTO for a single workout session inside an AI-generated onboarding plan.
 */
@Description("A single workout session within the fitness plan")
public record SessionDto(
        @Description("1-based position of this session within the overall plan (e.g., 1 for the first session)")
        int sequenceNumber,

        @Description("Short descriptive name for the session, e.g., 'Upper Body Strength' or 'Active Recovery'")
        String name,

        @Description("List of exercises to perform in this session. Must contain at least 2 exercises.")
        List<ExerciseDto> exercises
) {}
