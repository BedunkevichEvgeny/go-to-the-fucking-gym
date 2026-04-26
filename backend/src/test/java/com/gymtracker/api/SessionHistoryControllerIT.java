package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.SessionHistoryItem;
import com.gymtracker.api.dto.SessionHistoryPage;
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionHistoryControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void getHistoryReturnsCreatedSessionPage() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 26), "Tire Flip", "Quick session");

        HttpResponse<String> response = getHistory("page=0&size=20");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items()).isNotEmpty();
        assertThat(historyPage.items().getFirst().sessionId()).isNotNull();
    }

    @Test
    void getHistoryReturnsSessionsInReverseChronologicalOrder() throws Exception {
        LoggedSessionDetail older = postFreeSession(LocalDate.of(2026, 4, 25), "Bench Press", "Older");
        LoggedSessionDetail newer = postFreeSession(LocalDate.of(2026, 4, 27), "Bench Press", "Newer");

        HttpResponse<String> response = getHistory("page=0&size=20");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> ids = historyPage.items().stream().map(item -> item.sessionId().toString()).toList();
        assertThat(ids.indexOf(newer.sessionId().toString())).isLessThan(ids.indexOf(older.sessionId().toString()));
    }

    @Test
    void getHistoryRespectsPagination() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 25), "Bench Press", "A");
        postFreeSession(LocalDate.of(2026, 4, 26), "Bench Press", "B");
        LoggedSessionDetail newest = postFreeSession(LocalDate.of(2026, 4, 27), "Bench Press", "C");

        HttpResponse<String> response = getHistory("page=0&size=1");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.page()).isEqualTo(0);
        assertThat(historyPage.size()).isEqualTo(1);
        assertThat(historyPage.totalItems()).isEqualTo(3);
        assertThat(historyPage.items()).hasSize(1);
        assertThat(historyPage.items().getFirst().sessionId()).isEqualTo(newest.sessionId());
    }

    @Test
    void getHistoryFiltersByDateRange() throws Exception {
        postFreeSession(LocalDate.of(2026, 3, 31), "Bench Press", "Out of range");
        postFreeSession(LocalDate.of(2026, 4, 10), "Bench Press", "In range 1");
        postFreeSession(LocalDate.of(2026, 4, 20), "Bench Press", "In range 2");
        postFreeSession(LocalDate.of(2026, 5, 1), "Bench Press", "Out of range");

        HttpResponse<String> response = getHistory("page=0&size=20&dateFrom=2026-04-01&dateTo=2026-04-30");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items())
                .extracting(SessionHistoryItem::sessionDate)
                .containsExactly(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 10));
    }

    @Test
    void getHistoryFiltersByExerciseNamePartialMatch() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 25), "Running", "Cardio day");
        postFreeSession(LocalDate.of(2026, 4, 26), "Bench Press", "Press day");

        HttpResponse<String> response = getHistory("page=0&size=20&exerciseName=bench");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items()).hasSize(1);
        assertThat(historyPage.items().getFirst().name()).isEqualTo("Press day");
    }

    @Test
    void getHistoryCombinesDateAndExerciseFilters() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 10), "Bench Press", "Bench in range");
        postFreeSession(LocalDate.of(2026, 4, 11), "Running", "Other exercise");
        postFreeSession(LocalDate.of(2026, 5, 10), "Bench Press", "Bench out of range");

        HttpResponse<String> response = getHistory("page=0&size=20&dateFrom=2026-04-01&dateTo=2026-04-30&exerciseName=bench");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items()).hasSize(1);
        assertThat(historyPage.items().getFirst().name()).isEqualTo("Bench in range");
    }

    @Test
    void getHistoryReturnsEmptyPageWhenUserHasNoSessions() throws Exception {
        HttpResponse<String> response = getHistory("page=0&size=20");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items()).isEmpty();
        assertThat(historyPage.totalItems()).isZero();
        assertThat(historyPage.page()).isEqualTo(0);
        assertThat(historyPage.size()).isEqualTo(20);
    }

    @Test
    void getHistoryReturnsContractShape() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 27), "Tire Flip", "Contract test");

        HttpResponse<String> response = getHistory("page=0&size=20");
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.page()).isGreaterThanOrEqualTo(0);
        assertThat(historyPage.size()).isGreaterThan(0);
        assertThat(historyPage.totalItems()).isGreaterThanOrEqualTo(1);

        var item = historyPage.items().getFirst();
        assertThat(item.sessionId()).isNotNull();
        assertThat(item.sessionDate()).isNotNull();
        assertThat(item.sessionType()).isIn(SessionType.FREE, SessionType.PROGRAM);
        assertThat(item.exerciseCount()).isGreaterThan(0);
        assertThat(item.totalDurationSeconds()).isNotNull();
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

    private HttpResponse<String> getHistory(String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions/history?" + query)))
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

