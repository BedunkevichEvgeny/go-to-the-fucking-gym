package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OnboardingContractCompatibilityIT {

    @LocalServerPort
    private int port;

    @Test
    void onboardingAccessGateResponseMatches002ContractShape() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/profile-goals/access-gate", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"canAccessProgramTracking\"");
        assertThat(response.body()).contains("\"reasonCode\"");
    }

    @Test
    void trackerEndpointsRetain001ContractShapeWithOnboardingPresent() throws Exception {
        HttpResponse<String> nextSessionResponse = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");
        assertThat(nextSessionResponse.statusCode()).isEqualTo(200);
        assertThat(nextSessionResponse.body()).contains("\"programSessionId\"");
        assertThat(nextSessionResponse.body()).contains("\"sequenceNumber\"");
        assertThat(nextSessionResponse.body()).contains("\"name\"");
        assertThat(nextSessionResponse.body()).contains("\"exercises\"");

        HttpResponse<String> historyResponse = getWithBasicAuth("/api/logged-sessions/history?page=0&size=20", "user1", "password1");
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(historyResponse.body()).contains("\"items\"");
        assertThat(historyResponse.body()).contains("\"page\"");
        assertThat(historyResponse.body()).contains("\"size\"");
        assertThat(historyResponse.body()).contains("\"totalItems\"");
    }

    @Test
    void existingUserWithoutOnboardingMetadataCanStillUse001Flows() throws Exception {
        HttpResponse<String> nextSessionResponse = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");
        assertThat(nextSessionResponse.statusCode()).isEqualTo(200);

        String freeSessionBody = """
                {
                  "sessionType": "FREE",
                  "programSessionId": null,
                  "sessionDate": "2026-05-02",
                  "name": "IC-006 regression",
                  "notes": null,
                  "totalDurationSeconds": 900,
                  "feelings": {"rating": 8, "comment": null},
                  "exerciseEntries": [
                    {
                      "exerciseId": null,
                      "customExerciseName": "Bench Press",
                      "exerciseName": "Bench Press",
                      "exerciseType": "STRENGTH",
                      "sets": [{"reps": 10, "isBodyWeight": false, "weightValue": 60, "weightUnit": "KG"}],
                      "cardioLaps": []
                    }
                  ]
                }
                """;

        HttpResponse<String> createLoggedSessionResponse = postWithBasicAuth(
                "/api/logged-sessions",
                freeSessionBody,
                "user1",
                "password1");
        assertThat(createLoggedSessionResponse.statusCode()).isEqualTo(201);
        assertThat(createLoggedSessionResponse.body()).contains("\"sessionId\"");

        HttpResponse<String> historyResponse = getWithBasicAuth("/api/logged-sessions/history?page=0&size=20", "user1", "password1");
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(historyResponse.body()).contains("\"items\"");

        HttpResponse<String> progressionResponse = getWithBasicAuth("/api/progression/Bench Press", "user1", "password1");
        assertThat(progressionResponse.statusCode()).isEqualTo(200);
        assertThat(progressionResponse.body()).contains("\"exerciseName\":\"Bench Press\"");
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

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}


