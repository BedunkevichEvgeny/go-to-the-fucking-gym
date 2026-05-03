package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WeightUnit;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileGoalsAcceptanceControllerIT {

    private static final UUID USER_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void acceptProposalActivatesProgramAndReturnsAcceptanceContract() throws Exception {
        UUID proposalId = persistProposedPlan(USER_1_ID, "Acceptance Day");

        HttpResponse<String> response = postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/accept",
                "{}",
                "user1",
                "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"proposalId\":\"" + proposalId + "\"");
        assertThat(response.body()).contains("\"activatedProgramId\"");
        assertThat(response.body()).contains("\"replacedProgramId\"");
        assertThat(response.body()).contains("\"activatedAt\"");

        HttpResponse<String> nextSession = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");
        assertThat(nextSession.statusCode()).isEqualTo(200);
        assertThat(nextSession.body()).contains("\"name\":\"Acceptance Day\"");
        assertThat(nextSession.body()).contains("\"exerciseName\":\"Bench Press\"");
    }

    @Test
    void acceptProposalReturnsNotFoundWhenProposalDoesNotExist() throws Exception {
        HttpResponse<String> response = postWithBasicAuth(
                "/api/profile-goals/proposals/" + UUID.randomUUID() + "/accept",
                "{}",
                "user1",
                "password1");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private UUID persistProposedPlan(UUID userId, String sessionName) {
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
                .proposalPayload(singleSessionPayload(sessionName))
                .build();
        entityManager.persist(proposal);
        entityManager.flush();
        return proposal.getId();
    }

    private String singleSessionPayload(String sessionName) {
        return """
                {
                  "sessions": [
                    {
                      "sequenceNumber": 1,
                      "name": "%s",
                      "exercises": [
                        {
                          "exerciseName": "Bench Press",
                          "exerciseType": "STRENGTH",
                          "targetSets": 3,
                          "targetReps": 8,
                          "targetWeight": 60,
                          "targetWeightUnit": "KG",
                          "targetDurationSeconds": null,
                          "targetDistance": null,
                          "targetDistanceUnit": null
                        }
                      ]
                    }
                  ]
                }
                """.formatted(sessionName);
    }

    private HttpResponse<String> postWithBasicAuth(String path, String body, String username, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
                .header("Authorization", basicAuth(username, password))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBasicAuth(String path, String username, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
                .header("Authorization", basicAuth(username, password))
                .header("Accept", "application/json")
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}

