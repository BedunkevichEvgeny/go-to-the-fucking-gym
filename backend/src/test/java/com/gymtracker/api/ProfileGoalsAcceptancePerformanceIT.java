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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T050: NFR-003 accept/activate performance test (p95 <= 3s).
 *
 * Verifies that accepting and activating a proposal completes within
 * the performance threshold in at least 95% of attempts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileGoalsAcceptancePerformanceIT {

    @LocalServerPort
    private int port;

    @Test
    void acceptanceOperationCompletesWithinPerformanceThreshold() throws Exception {
        // Given: multiple proposals created by the same user
        List<Long> latencies = new ArrayList<>();
        int testCount = 20;

        for (int i = 0; i < testCount; i++) {
            // Create proposal
            String createBody = """
                    {"age":30,"currentWeight":80.5,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                    """;
            HttpResponse<String> createResponse = postWithBasicAuth("/api/profile-goals/onboarding", createBody, "perftest-user", "password");
            assertThat(createResponse.statusCode()).isEqualTo(200);

            String proposalId = extractProposalId(createResponse.body());

            // Measure acceptance latency
            long startTime = System.currentTimeMillis();

            HttpRequest acceptRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl("/api/profile-goals/proposals/" + proposalId + "/accept")))
                    .header("Authorization", basicAuth("perftest-user", "password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> acceptResponse = HttpClient.newHttpClient().send(acceptRequest, HttpResponse.BodyHandlers.ofString());

            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            latencies.add(latency);

            assertThat(acceptResponse.statusCode()).isEqualTo(200);
        }

        // Verify: p95 latency is <= 3000 ms
        latencies.sort(null);
        int p95Index = (int) Math.ceil(0.95 * latencies.size()) - 1;
        long p95Latency = latencies.get(p95Index);

        System.out.println("Acceptance operation latencies (ms):");
        System.out.println("  Mean: " + latencies.stream().mapToLong(Long::longValue).average().getAsDouble());
        System.out.println("  Min: " + latencies.get(0));
        System.out.println("  Max: " + latencies.get(latencies.size() - 1));
        System.out.println("  p95: " + p95Latency);

        assertThat(p95Latency).isLessThanOrEqualTo(3000L);
    }

    @Test
    void acceptanceOfMultiSessionProposalIsReasonablyFast() throws Exception {
        // Given: a proposal (acceptance should be consistently fast regardless of complexity)
        String body = """
                {"age":35,"currentWeight":85.0,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                """;
        HttpResponse<String> createResponse = postWithBasicAuth("/api/profile-goals/onboarding", body, "perf-user-2", "password");
        String proposalId = extractProposalId(createResponse.body());

        // When: accepting proposal
        long startTime = System.currentTimeMillis();

        HttpRequest acceptRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/profile-goals/proposals/" + proposalId + "/accept")))
                .header("Authorization", basicAuth("perf-user-2", "password"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> acceptResponse = HttpClient.newHttpClient().send(acceptRequest, HttpResponse.BodyHandlers.ofString());

        long endTime = System.currentTimeMillis();
        long latency = endTime - startTime;

        // Then: acceptance should be quick even for multi-session proposals
        assertThat(acceptResponse.statusCode()).isEqualTo(200);
        assertThat(latency).isLessThan(3000L);
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

    private String extractProposalId(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var response = mapper.readValue(responseBody, java.util.Map.class);
        return (String) response.get("proposalId");
    }
}

