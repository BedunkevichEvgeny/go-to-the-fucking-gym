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
class ProfileGoalsControllerIT {

    @LocalServerPort
    private int port;

    @Test
    void onboardingSubmissionReturnsValidationFailureForInvalidPayload() throws Exception {
        String body = """
                {"age":12,"currentWeight":0,"weightUnit":"KG","primaryGoal":"LOSE_WEIGHT"}
                """;

        HttpResponse<String> response = postWithBasicAuth("/api/profile-goals/onboarding", body, "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void onboardingSubmissionReturnsProposalForValidPayload() throws Exception {
        String body = """
                {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;

        HttpResponse<String> response = postWithBasicAuth("/api/profile-goals/onboarding", body, "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("proposalId");
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

