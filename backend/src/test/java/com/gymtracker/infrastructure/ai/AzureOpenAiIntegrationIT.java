package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "azure.openai.endpoint=https://integration.test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-gpt-deployment"
})
class AzureOpenAiIntegrationIT {

    @Autowired
    private LangChainSessionProcessor processor;

    @Test
    void processorInitializesFromAzureEnvironmentProperties() {
        assertThat(processor.endpoint()).isEqualTo("https://integration.test.azure.openai");
        assertThat(processor.deployment()).isEqualTo("test-gpt-deployment");
    }

    @Test
    void workflowAcceptsSessionSummaryAndBuildsPromptWithSessionData() {
        SessionSummaryDTO summary = sampleSummary();

        String prompt = processor.buildPrompt(summary);
        String response = processor.process(summary);

        assertThat(prompt).contains("Session: " + summary.sessionId());
        assertThat(prompt).contains("Bench Press");
        assertThat(prompt).contains("target=");
        assertThat(prompt).contains("Feeling rating: 8");
        assertThat(response).contains("azure-openai[");
        assertThat(response).contains("deployment=test-gpt-deployment");
    }

    @Test
    void workflowReturnsCapturedResponseWithinTimeoutBudget() {
        SessionSummaryDTO summary = sampleSummary();

        String response = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> processor.process(summary));

        assertThat(response).isNotBlank();
    }

    private SessionSummaryDTO sampleSummary() {
        return new SessionSummaryDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.randomUUID(),
                SessionType.PROGRAM,
                LocalDate.of(2026, 4, 27),
                1800,
                new SessionSummaryDTO.FeelingsSummary(8, "Solid session"),
                new SessionSummaryDTO.UserPreferences("KG"),
                List.of(new SessionSummaryDTO.ExerciseSummary(
                        "Bench Press",
                        ExerciseType.STRENGTH,
                        new SessionSummaryDTO.ActualPerformance(3, 24, new BigDecimal("80"), null, null),
                        new SessionSummaryDTO.TargetPerformance(3, 8, new BigDecimal("82.5"), null, null))));
    }
}


