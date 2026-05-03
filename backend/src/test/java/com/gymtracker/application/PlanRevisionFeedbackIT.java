package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// ===== T071-BUG-B1: Integration test — requestedChanges reaches AI revision prompt =====

/**
 * Integration test verifying that {@code requestedChanges} sent to the reject endpoint
 * is forwarded to {@link OnboardingPlanGenerator#generateRevision} and that blank
 * feedback causes a 400 validation error (never reaching the service).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlanRevisionFeedbackIT {

    private static final UUID USER_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private OnboardingPlanGenerator onboardingPlanGenerator;

    // ── T071-1: non-blank requestedChanges reaches generateRevision() ─────────

    @Test
    void rejectEndpoint_WithNonBlankFeedback_CallsGenerateRevisionWithExactFeedbackString() throws Exception {
        String feedback = "make it shorter";
        UUID proposalId = persistProposedPlan(USER_1_ID, "Leg Day");

        stubGeneratorForRevision(proposalId, feedback);

        HttpResponse<String> response = postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/reject",
                "{\"requestedChanges\": \"" + feedback + "\"}",
                "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);

        ArgumentCaptor<String> feedbackCaptor = ArgumentCaptor.forClass(String.class);
        verify(onboardingPlanGenerator).generateRevision(eq(USER_1_ID), any(), feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue()).isEqualTo(feedback);
    }

    @Test
    void rejectEndpoint_WithNonBlankFeedback_DoesNotCallGenerateInitialProposal() throws Exception {
        String feedback = "add more cardio";
        UUID proposalId = persistProposedPlan(USER_1_ID, "Upper Body");

        stubGeneratorForRevision(proposalId, feedback);

        postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/reject",
                "{\"requestedChanges\": \"" + feedback + "\"}",
                "user1", "password1");

        verify(onboardingPlanGenerator, org.mockito.Mockito.never())
                .generateInitialProposal(any(), any());
    }

    // ── T071-2: blank requestedChanges → 400 validation (never reaches service) ──

    @Test
    void rejectEndpoint_WithBlankFeedback_Returns400ValidationError() throws Exception {
        UUID anyProposalId = UUID.randomUUID();

        HttpResponse<String> response = postWithBasicAuth(
                "/api/profile-goals/proposals/" + anyProposalId + "/reject",
                "{\"requestedChanges\": \"\"}",
                "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubGeneratorForRevision(UUID proposalId, String feedback) {
        PlanProposalResponse stub = new PlanProposalResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                ProposalStatus.PROPOSED,
                new GeneratedBy(ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of());
        when(onboardingPlanGenerator.generateRevision(eq(USER_1_ID), any(), eq(feedback)))
                .thenReturn(stub);
    }

    private UUID persistProposedPlan(UUID userId, String sessionName) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                    .userId(userId)
                    .age(30)
                    .currentWeight(new BigDecimal("80.0"))
                    .weightUnit(WeightUnit.KG)
                    .primaryGoal(OnboardingPrimaryGoal.STRENGTH)
                    .status(OnboardingAttemptStatus.IN_PROGRESS)
                    .build();
            entityManager.persist(attempt);

            PlanProposal proposal = PlanProposal.builder()
                    .attempt(attempt)
                    .userId(userId)
                    .version(1)
                    .status(ProposalStatus.PROPOSED)
                    .provider(ProposalProvider.AZURE_OPENAI)
                    .modelDeployment("gpt-35-turbo")
                    .proposalPayload(minimalPayload(sessionName))
                    .build();
            entityManager.persist(proposal);
            entityManager.flush();
            return proposal.getId();
        });
    }

    private String minimalPayload(String sessionName) {
        return """
                {
                  "sessions": [
                    {
                      "sequenceNumber": 1,
                      "name": "%s",
                      "exercises": []
                    }
                  ]
                }
                """.formatted(sessionName);
    }

    private HttpResponse<String> postWithBasicAuth(String path, String body, String username, String password)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", basicAuth(username, password))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}








