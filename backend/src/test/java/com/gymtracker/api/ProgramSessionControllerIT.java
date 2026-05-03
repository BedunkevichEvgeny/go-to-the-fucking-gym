package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgramSessionControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getNextProgramSessionReturnsSessionWithTargets() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Upper Body\"");
        assertThat(response.body()).contains("\"exerciseName\":\"Bench Press\"");
        assertThat(response.body()).contains("\"exercises\"");
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenNoActiveProgram() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenProgramCompleted() throws Exception {
        var program = workoutProgramRepository
                .findFirstByUserIdAndStatus(
                        java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        ProgramStatus.ACTIVE)
                .orElseThrow();
        var sessions = programSessionRepository.findByProgram_IdOrderBySequenceNumberAsc(program.getId());
        sessions.forEach(session -> session.setCompleted(true));
        programSessionRepository.saveAll(sessions);
        program.setStatus(ProgramStatus.COMPLETED);
        program.setCompletedAt(OffsetDateTime.now());
        workoutProgramRepository.save(program);

        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void getNextProgramSessionRequiresAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/program-sessions/next")))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void getNextProgramSessionEnforcesCrossUserIsolation() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void getNextProgramSessionRemainsContractCompatibleAfterOnboardingActivation() throws Exception {
        UUID proposalId = persistProposedPlanForUser1("Activated Session");
        HttpResponse<String> acceptResponse = postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/accept",
                "{}",
                "user1",
                "password1");
        assertThat(acceptResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"programSessionId\"");
        assertThat(response.body()).contains("\"sequenceNumber\":1");
        assertThat(response.body()).contains("\"name\":\"Activated Session\"");
        assertThat(response.body()).contains("\"exercises\"");
        assertThat(response.body()).contains("\"exerciseType\":\"STRENGTH\"");
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

    private HttpResponse<String> postWithBasicAuth(String path, String body, String username, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
                .header("Authorization", basicAuth(username, password))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private UUID persistProposedPlanForUser1(String sessionName) {
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .age(29)
                .currentWeight(new BigDecimal("78.5"))
                .weightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingPrimaryGoal.STRENGTH)
                .status(OnboardingAttemptStatus.IN_PROGRESS)
                .build();
        entityManager.persist(attempt);

        PlanProposal proposal = PlanProposal.builder()
                .attempt(attempt)
                .userId(attempt.getUserId())
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
                          "targetSets": 4,
                          "targetReps": 6,
                          "targetWeight": 75,
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

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
