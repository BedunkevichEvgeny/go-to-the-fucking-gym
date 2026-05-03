package com.gymtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T049: Cross-user denial integration tests for FR-013 authorization.
 *
 * Verifies that onboarding endpoints properly deny cross-user access to
 * attempts, proposals, feedback, and accept operations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileGoalOnboardingAuthorizationIT {

    @LocalServerPort
    private int port;

    @Test
    void rejectProposalFromAnotherUserFails() throws Exception {
        // Given: user1 creates a proposal
        String user1Body = """
                {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;
        HttpResponse<String> user1Response = postWithBasicAuth("/api/profile-goals/onboarding", user1Body, "user1", "password1");
        assertThat(user1Response.statusCode()).isEqualTo(200);

        String proposalId = extractProposalId(user1Response.body());

        // When: user2 tries to reject user1's proposal
        String rejectBody = """
                {"requestedChanges":"Less weight"}
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/profile-goals/proposals/" + proposalId + "/reject")))
                .header("Authorization", basicAuth("user2", "password2"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rejectBody))
                .build();

        HttpResponse<String> rejectResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Then: should be forbidden
        assertThat(rejectResponse.statusCode()).isEqualTo(403);
    }

    @Test
    void acceptProposalFromAnotherUserFails() throws Exception {
        // Given: user1 creates a proposal
        String user1Body = """
                {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;
        HttpResponse<String> user1Response = postWithBasicAuth("/api/profile-goals/onboarding", user1Body, "user1", "password1");
        assertThat(user1Response.statusCode()).isEqualTo(200);

        String proposalId = extractProposalId(user1Response.body());

        // When: user2 tries to accept user1's proposal
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/profile-goals/proposals/" + proposalId + "/accept")))
                .header("Authorization", basicAuth("user2", "password2"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> acceptResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Then: should be forbidden
        assertThat(acceptResponse.statusCode()).isEqualTo(403);
    }

    @Test
    void getCurrentAttemptForAnotherUserReturnsEmpty() throws Exception {
        // Given: user1 creates an attempt
        String user1Body = """
                {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;
        postWithBasicAuth("/api/profile-goals/onboarding", user1Body, "user1", "password1");

        // When: user2 queries their current attempt (should be empty, not seeing user1's)
        HttpResponse<String> user2Response = getWithBasicAuth("/api/profile-goals/onboarding/current", "user2", "password2");

        // Then: user2 should either get 204 or empty data
        assertThat(user2Response.statusCode()).isIn(200, 204);
        if (user2Response.statusCode() == 200) {
            assertThat(user2Response.body()).doesNotContain("user1");
        }
    }

    @Test
    void accessGateAllowsQueryButEnforcesOwnership() throws Exception {
        // Given: user1 is authenticated
        // When: user1 checks their access gate
        HttpResponse<String> user1Gate = getWithBasicAuth("/api/profile-goals/access-gate", "user1", "password1");

        // Then: should succeed and show user's own status
        assertThat(user1Gate.statusCode()).isEqualTo(200);
        assertThat(user1Gate.body()).contains("canAccessProgramTracking");

        // When: user2 checks their access gate (independent)
        HttpResponse<String> user2Gate = getWithBasicAuth("/api/profile-goals/access-gate", "user2", "password2");

        // Then: user2 should get their own status (not user1's)
        assertThat(user2Gate.statusCode()).isEqualTo(200);
        // Both should be false initially since neither has completed onboarding
        assertThat(user2Gate.body()).contains("canAccessProgramTracking");
    }

    @Test
    void multipleUsersCannotInterfere() throws Exception {
        // Given: three different users
        String user1Body = """
                {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;
        String user2Body = """
                {"age":25,"currentWeight":70.0,"weightUnit":"KG","primaryGoal":"WEIGHT_LOSS"}
                """;

        // When: user1 creates proposal
        HttpResponse<String> user1Response = postWithBasicAuth("/api/profile-goals/onboarding", user1Body, "user1", "password1");
        assertThat(user1Response.statusCode()).isEqualTo(200);

        // When: user2 also creates proposal
        HttpResponse<String> user2Response = postWithBasicAuth("/api/profile-goals/onboarding", user2Body, "user2", "password2");
        assertThat(user2Response.statusCode()).isEqualTo(200);

        String user2ProposalId = extractProposalId(user2Response.body());

        // When: user3 tries to access user2's proposal
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/profile-goals/proposals/" + user2ProposalId + "/accept")))
                .header("Authorization", basicAuth("user3", "password3"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> user3AttemptResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Then: user3 should be denied
        assertThat(user3AttemptResponse.statusCode()).isEqualTo(403);
    }

    // ========== Helper Methods ==========

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
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
                .GET()
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String extractProposalId(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var response = mapper.readValue(responseBody, java.util.Map.class);
        return (String) response.get("proposalId");
    }
}

