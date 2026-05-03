package com.gymtracker.infrastructure.ai;

import com.gymtracker.infrastructure.ai.dto.OnboardingPlanDto;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j {@code AiServices}-driven assistant interface for workout session analysis
 * and onboarding plan generation.
 *
 * <p>The runtime implementation is produced by {@code AiServices.builder(LangChainSessionProcessor.class)}
 * in {@link com.gymtracker.infrastructure.config.AiChatModelConfig}.
 *
 * <p>Method parameters:
 * <ul>
 *   <li>{@code memoryId} – per-user/session memory key used by {@code ChatMemoryProvider}
 *       to scope conversation history.</li>
 *   <li>{@code userMessage} – the full prompt text sent to the model as the user turn.</li>
 * </ul>
 */
public interface LangChainSessionProcessor {

    /**
     * Free-text session analysis — used by {@link AiHandoffService} for progression coaching.
     * Returns the raw model output as a {@code String}.
     */
    @SystemMessage("""
            You are a workout progression coach and fitness AI assistant.
            Analyse workout sessions and provide evidence-based progression insights,
            or generate personalised fitness plans based on user profiles.
            Always return your response in the exact format requested by the user message.
            Never add markdown code blocks or any wrapper text — return raw content only.
            """)
    String process(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * Structured-output onboarding plan generation — LangChain4j {@code AiServices} automatically
     * generates a JSON schema from {@link OnboardingPlanDto} and instructs the model to conform to it.
     * Jackson deserializes the response; unknown {@code ExerciseType} values fall back to
     * {@code STRENGTH} via {@link com.gymtracker.infrastructure.ai.dto.SafeExerciseTypeDeserializer}.
     */
    @SystemMessage("""
            You are a personalised fitness planning AI.
            Generate a structured onboarding workout plan strictly as a JSON object matching the
            schema provided. Do NOT add any markdown, prose, or wrapper text — return raw JSON only.
            """)
    OnboardingPlanDto processOnboarding(@MemoryId String memoryId, @UserMessage String userMessage);
}
