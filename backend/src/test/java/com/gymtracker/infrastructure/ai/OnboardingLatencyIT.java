package com.gymtracker.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OnboardingLatencyIT {

    private static final long ONBOARDING_P95_THRESHOLD_MS = 10_000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void onboardingCreateEndpointMeetsP95Threshold() throws Exception {
        List<Long> latencies = new ArrayList<>();
        int attempts = 20;

        for (int i = 0; i < attempts; i++) {
            String username = "latency-user-" + i;
            String body = """
                    {"age":30,"currentWeight":78.0,"weightUnit":"KG","primaryGoal":"STRENGTH"}
                    """;

            long startedAt = System.currentTimeMillis();
            HttpResponse<String> response = postCreateOnboarding(body, username);
            long latency = System.currentTimeMillis() - startedAt;

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(extractProposalId(response.body())).isNotBlank();
            latencies.add(latency);
        }

        long p95 = percentile(latencies, 0.95d);
        long p50 = percentile(latencies, 0.50d);

        // Basic instrumentation to make p95 evidence visible in test logs.
        System.out.println("Onboarding create latency stats (ms): p50=" + p50 + ", p95=" + p95 + ", max=" + latencies.stream().mapToLong(Long::longValue).max().orElse(0L));
        assertThat(p95).isLessThanOrEqualTo(ONBOARDING_P95_THRESHOLD_MS);
    }

    private HttpResponse<String> postCreateOnboarding(String body, String username) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/profile-goals/onboarding")))
                .header("Authorization", basicAuth(username, "password"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private long percentile(List<Long> values, double quantile) {
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private String extractProposalId(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var response = mapper.readValue(responseBody, java.util.Map.class);
        return (String) response.get("proposalId");
    }
}


