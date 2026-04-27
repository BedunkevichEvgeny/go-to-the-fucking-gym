package com.gymtracker.infrastructure.ai;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LangChainSessionProcessor {

    private static final Logger log = LoggerFactory.getLogger(LangChainSessionProcessor.class);

    private final String endpoint;
    private final String apiKey;
    private final String deployment;
    private final Duration timeout;
    private final int maxAttempts;

    public LangChainSessionProcessor(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.deployment}") String deployment,
            @Value("${ai.handoff.timeout-seconds:30}") int timeoutSeconds,
            @Value("${ai.handoff.max-attempts:3}") int maxAttempts
    ) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.deployment = deployment;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public String process(SessionSummaryDTO summary) {
        String prompt = buildPrompt(summary);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String response = callAzureOpenAiWithTimeout(prompt);
                log.info("AI handoff processed session {} for user {} using deployment {}",
                        summary.sessionId(), summary.userId(), deployment);
                return response;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (!isTransientFailure(failure) || attempt == maxAttempts) {
                    throw failure;
                }
                log.warn("Transient Azure OpenAI failure on attempt {}/{} for session {}. Retrying.",
                        attempt, maxAttempts, summary.sessionId());
                sleepBeforeRetry();
            }
        }

        throw lastFailure == null ? new IllegalStateException("Unexpected AI handoff failure") : lastFailure;
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<String> processAsync(SessionSummaryDTO summary) {
        return CompletableFuture.completedFuture(process(summary));
    }

    String buildPrompt(SessionSummaryDTO summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("Analyze workout session for progression coaching.\n")
                .append("User: ").append(summary.userId()).append('\n')
                .append("Session: ").append(summary.sessionId()).append('\n')
                .append("Type: ").append(summary.sessionType()).append('\n')
                .append("Date: ").append(summary.sessionDate()).append('\n')
                .append("Payload: ").append(summary.toPromptPayload()).append('\n')
                .append("Preferred weight unit: ").append(summary.metadata().preferredWeightUnit()).append('\n');

        if (summary.feelings() != null && summary.feelings().rating() != null) {
            builder.append("Feeling rating: ").append(summary.feelings().rating()).append('\n');
            if (summary.feelings().comment() != null && !summary.feelings().comment().isBlank()) {
                builder.append("Feeling comment: ").append(summary.feelings().comment()).append('\n');
            }
        }

        builder.append("Exercises:\n");
        for (SessionSummaryDTO.ExerciseSummary exercise : summary.exercises()) {
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

    private String callAzureOpenAiWithTimeout(String prompt) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> callAzureOpenAi(prompt))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new IllegalStateException("Azure OpenAI call timed out", timeoutException);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Azure OpenAI call failed", cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Azure OpenAI call interrupted", interruptedException);
        }
    }

    String callAzureOpenAi(String prompt) {
        String keyMarker = apiKey == null ? "missing" : "present";
        return "azure-openai[endpoint=" + endpoint + ",deployment=" + deployment + ",apiKey=" + keyMarker
                + "]: accepted prompt hash=" + prompt.hashCode();
    }

    private boolean isTransientFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timeout") || normalized.contains("429") || normalized.contains("503");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", interruptedException);
        }
    }

    String endpoint() {
        return endpoint;
    }

    String deployment() {
        return deployment;
    }
}

