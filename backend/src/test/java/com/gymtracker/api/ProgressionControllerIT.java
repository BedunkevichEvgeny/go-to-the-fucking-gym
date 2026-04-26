package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgressionControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void getProgressionReturnsExerciseNameAndChronologicalPoints() throws Exception {
        postFreeStrengthSession(LocalDate.of(2026, 4, 27), "Deadlift", 120, "Progress newest");
        postFreeStrengthSession(LocalDate.of(2026, 4, 25), "Deadlift", 110, "Progress oldest");

        HttpResponse<String> response = getProgression("Deadlift");
        ProgressionResponse progression = objectMapper.readValue(response.body(), ProgressionResponse.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(progression.exerciseName()).isEqualTo("Deadlift");
        assertThat(progression.points()).hasSize(2);
        assertThat(progression.points().get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(progression.points().get(1).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 27));
    }

    @Test
    void getProgressionIncludesMetricTypeAndValue() throws Exception {
        postFreeStrengthSession(LocalDate.of(2026, 4, 27), "Bench Press", 70, "Bench session");

        HttpResponse<String> response = getProgression("bench press");
        ProgressionResponse progression = objectMapper.readValue(response.body(), ProgressionResponse.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(progression.points()).hasSize(1);
        assertThat(progression.points().getFirst().metricType()).isEqualTo(ProgressionMetricType.WEIGHT);
        assertThat(progression.points().getFirst().metricValue()).isEqualTo(70.0);
    }

    @Test
    void getProgressionForUnknownExerciseReturnsEmptyPointsArray() throws Exception {
        postFreeStrengthSession(LocalDate.of(2026, 4, 27), "Squat", 100, "Leg day");

        HttpResponse<String> response = getProgression("Unknown Exercise");
        ProgressionResponse progression = objectMapper.readValue(response.body(), ProgressionResponse.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(progression.exerciseName()).isEqualTo("Unknown Exercise");
        assertThat(progression.points()).isEmpty();
    }

    @Test
    void getProgressionResponseMatchesContractShape() throws Exception {
        postFreeStrengthSession(LocalDate.of(2026, 4, 27), "Row", 80, "Row session");

        HttpResponse<String> response = getProgression("Row");
        ProgressionResponse progression = objectMapper.readValue(response.body(), ProgressionResponse.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(progression.exerciseName()).isNotBlank();
        assertThat(progression.points()).isNotEmpty();
        assertThat(progression.points().getFirst().sessionId()).isNotNull();
        assertThat(progression.points().getFirst().sessionDate()).isNotNull();
        assertThat(progression.points().getFirst().metricType()).isIn(
                ProgressionMetricType.WEIGHT,
                ProgressionMetricType.DURATION,
                ProgressionMetricType.DISTANCE);
    }

    private LoggedSessionDetail postFreeStrengthSession(LocalDate date, String exerciseName, int weight, String sessionName) throws Exception {
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
                        List.of(new StrengthSetInput(6, false, BigDecimal.valueOf(weight), WeightUnit.KG)),
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

    private HttpResponse<String> getProgression(String exerciseName) throws Exception {
        String encodedName = URLEncoder.encode(exerciseName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/progression/" + encodedName)))
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


