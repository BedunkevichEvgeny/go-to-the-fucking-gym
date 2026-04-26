package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ApiError;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionDetailControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void getSessionDetailReturnsSavedSessionWithExercisesSetsAndFeelings() throws Exception {
        LoggedSessionDetail created = postFreeSession("user1", "password1", LocalDate.of(2026, 4, 27), "Bench Press", "Push day");

        HttpResponse<String> response = getSessionDetail("user1", "password1", created.sessionId());
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(detail.sessionId()).isEqualTo(created.sessionId());
        assertThat(detail.sessionType()).isEqualTo(SessionType.FREE);
        assertThat(detail.feelings()).isNotNull();
        assertThat(detail.feelings().rating()).isEqualTo(8);
        assertThat(detail.exerciseEntries()).hasSize(1);
        assertThat(detail.exerciseEntries().getFirst().exerciseName()).isEqualTo("Bench Press");
        assertThat(detail.exerciseEntries().getFirst().sets()).hasSize(1);
        assertThat(detail.exerciseEntries().getFirst().sets().getFirst().reps()).isEqualTo(12);
    }

    @Test
    void getSessionDetailReturnsNotFoundForUnknownSession() throws Exception {
        HttpResponse<String> response = getSessionDetail("user1", "password1", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        ApiError error = objectMapper.readValue(response.body(), ApiError.class);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(error.code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void getSessionDetailReturnsForbiddenForNonOwner() throws Exception {
        LoggedSessionDetail user2Session = postFreeSession("user2", "password2", LocalDate.of(2026, 4, 27), "Deadlift", "User2 day");

        HttpResponse<String> response = getSessionDetail("user1", "password1", user2Session.sessionId());
        ApiError error = objectMapper.readValue(response.body(), ApiError.class);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(error.code()).isEqualTo("FORBIDDEN");
    }

    private LoggedSessionDetail postFreeSession(
            String username,
            String password,
            LocalDate date,
            String exerciseName,
            String sessionName
    ) throws Exception {
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.FREE,
                null,
                date,
                sessionName,
                null,
                1200,
                new SessionFeelingsInput(8, "Solid workout"),
                List.of(new ExerciseEntryInput(
                        null,
                        exerciseName,
                        exerciseName,
                        ExerciseType.STRENGTH,
                        List.of(new StrengthSetInput(12, false, new BigDecimal("60"), WeightUnit.KG)),
                        List.of())));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions")))
                .header("Authorization", basicAuth(username, password))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readValue(response.body(), LoggedSessionDetail.class);
    }

    private HttpResponse<String> getSessionDetail(String username, String password, UUID sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions/" + sessionId)))
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

