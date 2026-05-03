package com.gymtracker.infrastructure.ai.dto;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;

/**
 * Top-level structured output DTO for a LangChain4j {@code AiServices}-generated onboarding plan.
 *
 * <p>LangChain4j automatically generates the JSON schema from this record's structure and
 * instructs the model to return output conforming to it.  Jackson deserializes the response
 * back into this record without any manual JSON parsing.
 */
@Description("A personalized fitness plan consisting of multiple workout sessions")
public record OnboardingPlanDto(
        @Description("Ordered list of workout sessions that make up the fitness plan. Must contain at least 3 sessions.")
        List<SessionDto> sessions
) {}
