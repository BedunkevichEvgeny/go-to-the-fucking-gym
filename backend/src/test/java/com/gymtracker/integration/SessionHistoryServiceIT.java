package com.gymtracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionAiSuggestion;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.SessionAiSuggestionRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionHistoryServiceIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Autowired
    private SessionAiSuggestionRepository sessionAiSuggestionRepository;

    @Test
    void getHistoryList_itemsDoNotContainAiSuggestionField() throws Exception {
        LoggedSessionDetail created = postFreeSession(LocalDate.of(2026, 5, 1), "Squat", "Leg day");
        UUID sessionId = created.sessionId();

        // Persist a suggestion so that if the mapper accidentally includes it, the test catches it
        SessionAiSuggestion suggestion = new SessionAiSuggestion();
        suggestion.setSession(loggedSessionRepository.getReferenceById(sessionId));
        suggestion.setSuggestion("Great leg session!");
        sessionAiSuggestionRepository.save(suggestion);

        HttpResponse<String> response = getHistory();
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode items = root.get("items");
        assertThat(items).isNotNull();
        assertThat(items.isArray()).isTrue();
        assertThat(items.isEmpty()).isFalse();
        items.forEach(item -> assertThat(item.has("aiSuggestion")).isFalse());
    }

    private LoggedSessionDetail postFreeSession(LocalDate date, String exerciseName, String sessionName) throws Exception {
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.FREE,
                null,
                date,
                sessionName,
                null,
                1200,
                new SessionFeelingsInput(8, null),
                List.of(new ExerciseEntryInput(
                        null,
                        exerciseName,
                        exerciseName,
                        ExerciseType.STRENGTH,
                        List.of(new StrengthSetInput(12, false, new BigDecimal("60"), WeightUnit.KG)),
                        List.of())));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions")))
                .header("Authorization", basicAuth("user1", "password1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readValue(response.body(), LoggedSessionDetail.class);
    }

    private HttpResponse<String> getHistory() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions/history?page=0&size=20")))
                .header("Authorization", basicAuth("user1", "password1"))
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
