package com.gymtracker.infrastructure.ai.dto;

import java.util.List;

/**
 * Top-level structured output DTO for a LangChain4j {@code AiServices}-generated onboarding plan.
 *
 * <p>LangChain4j automatically generates the JSON schema from this record's structure and
 * instructs the model to return output conforming to it.  Jackson deserializes the response
 * back into this record without any manual JSON parsing.
 */
public record OnboardingPlanDto(List<SessionDto> sessions) {}

