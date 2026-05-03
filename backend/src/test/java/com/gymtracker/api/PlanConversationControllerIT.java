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

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "azure.openai.endpoint=http://localhost:8080",
                "azure.openai.api-key=fake-key",
                "azure.openai.deployment=gpt-35-turbo"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlanConversationControllerIT {

    @LocalServerPort
    private int port;

    @Test
    void rejectAndReviseReturnsRevisedProposal() throws Exception {
        String initialBody = """
                {"age":31,"currentWeight":83.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;

        HttpResponse<String> createResponse = postWithBasicAuth("/api/profile-goals/onboarding", initialBody, "user1", "password1");
        assertThat(createResponse.statusCode()).isEqualTo(200);

        String proposalId = extractProposalId(createResponse.body());
        String rejectBody = """
                {"requestedChanges":"Less deadlift volume and more rowing"}
                """;

        HttpResponse<String> revisedResponse = postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/reject",
                rejectBody,
                "user1",
                "password1");

        assertThat(revisedResponse.statusCode()).isEqualTo(200);
        assertThat(revisedResponse.body()).contains("\"version\":2");
    }

    @Test
    void rejectAndReviseRejectsInvalidFeedback() throws Exception {
        String initialBody = """
                {"age":33,"currentWeight":90,"weightUnit":"KG","primaryGoal":"BUILD_MUSCLES"}
                """;

        HttpResponse<String> createResponse = postWithBasicAuth("/api/profile-goals/onboarding", initialBody, "user1", "password1");
        assertThat(createResponse.statusCode()).isEqualTo(200);

        String proposalId = extractProposalId(createResponse.body());
        String invalidRejectBody = """
                {"requestedChanges":""}
                """;

        HttpResponse<String> revisedResponse = postWithBasicAuth(
                "/api/profile-goals/proposals/" + proposalId + "/reject",
                invalidRejectBody,
                "user1",
                "password1");

        assertThat(revisedResponse.statusCode()).isEqualTo(400);
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

    private String extractProposalId(String json) {
        String marker = "\"proposalId\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("proposalId missing from payload: " + json);
        }
        int from = start + marker.length();
        int end = json.indexOf('"', from);
        return json.substring(from, end);
    }
}
