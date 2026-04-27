package com.gymtracker.infrastructure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LangChainSessionProcessor {

    private static final Logger log = LoggerFactory.getLogger(LangChainSessionProcessor.class);

    private final String endpoint;
    private final String apiKey;
    private final String deployment;

    public LangChainSessionProcessor(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.deployment}") String deployment
    ) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.deployment = deployment;
    }

    public String process(SessionSummaryDto summary) {
        String prompt = buildPrompt(summary);
        String response = callAzureOpenAi(prompt);
        log.info("AI handoff processed session {} for user {} using deployment {}",
                summary.sessionId(), summary.userId(), deployment);
        return response;
    }

    String buildPrompt(SessionSummaryDto summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("Analyze workout session for progression coaching.\n")
                .append("User: ").append(summary.userId()).append('\n')
                .append("Session: ").append(summary.sessionId()).append('\n')
                .append("Type: ").append(summary.sessionType()).append('\n')
                .append("Date: ").append(summary.sessionDate()).append('\n')
                .append("Preferred weight unit: ").append(summary.metadata().preferredWeightUnit()).append('\n');

        if (summary.feelings() != null && summary.feelings().rating() != null) {
            builder.append("Feeling rating: ").append(summary.feelings().rating()).append('\n');
            if (summary.feelings().comment() != null && !summary.feelings().comment().isBlank()) {
                builder.append("Feeling comment: ").append(summary.feelings().comment()).append('\n');
            }
        }

        builder.append("Exercises:\n");
        for (SessionSummaryDto.ExerciseSummary exercise : summary.exercises()) {
            builder.append("- ").append(exercise.exerciseName())
                    .append(" [").append(exercise.exerciseType()).append("]")
                    .append(" actual=").append(exercise.actual());
            if (exercise.target() != null) {
                builder.append(" target=").append(exercise.target());
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    String callAzureOpenAi(String prompt) {
        String keyMarker = apiKey == null ? "missing" : "present";
        return "azure-openai[endpoint=" + endpoint + ",deployment=" + deployment + ",apiKey=" + keyMarker
                + "]: accepted prompt hash=" + prompt.hashCode();
    }

    String endpoint() {
        return endpoint;
    }

    String deployment() {
        return deployment;
    }
}

