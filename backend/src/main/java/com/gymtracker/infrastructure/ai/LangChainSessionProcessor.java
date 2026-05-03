package com.gymtracker.infrastructure.ai;

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

    @SystemMessage("""
            You are a workout progression coach and fitness AI assistant.
            Analyse workout sessions and provide evidence-based progression insights,
            or generate personalised fitness plans based on user profiles.
            Always return your response in the exact format requested by the user message.
            Never add markdown code blocks or any wrapper text — return raw content only.
            """)
    String process(@MemoryId String memoryId, @UserMessage String userMessage);
}
