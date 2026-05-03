package com.gymtracker.infrastructure.config;

import com.gymtracker.infrastructure.ai.LangChainSessionProcessor;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures LangChain4j beans: {@link ChatModel}, {@link ChatMemoryProvider},
 * and the {@link LangChainSessionProcessor} AiServices proxy backed by Azure OpenAI.
 */
@Configuration
public class AiChatModelConfig {

    @Bean
    public ChatModel azureOpenAiChatModel(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.deployment}") String deployment,
            @Value("${ai.handoff.timeout-seconds:30}") int timeoutSeconds
    ) {
        return AzureOpenAiChatModel.builder()
                .endpoint(endpoint)
                .apiKey(apiKey)
                .deploymentName(deployment)
                .timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)))
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(50)
                .build();
    }

    @Bean
    public LangChainSessionProcessor langChainSessionProcessor(
            ChatModel chatModel,
            ChatMemoryProvider chatMemoryProvider
    ) {
        return AiServices.builder(LangChainSessionProcessor.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }
}

