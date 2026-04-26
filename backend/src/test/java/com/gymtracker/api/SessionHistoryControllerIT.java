package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
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
        postFreeSession(LocalDate.of(2026, 4, 26));

        HttpResponse<String> response = getHistory();
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.items()).isNotEmpty();
        assertThat(historyPage.items().getFirst().sessionId()).isNotNull();
    }

    @Test
    void getHistoryReturnsSessionsInReverseChronologicalOrder() throws Exception {
        LoggedSessionDetail older = postFreeSession(LocalDate.of(2026, 4, 25));
        LoggedSessionDetail newer = postFreeSession(LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = getHistory();
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> ids = historyPage.items().stream().map(item -> item.sessionId().toString()).toList();
        assertThat(ids.indexOf(newer.sessionId().toString())).isLessThan(ids.indexOf(older.sessionId().toString()));
    }

    @Test
    void getHistoryReturnsContractShape() throws Exception {
        postFreeSession(LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = getHistory();
        SessionHistoryPage historyPage = objectMapper.readValue(response.body(), SessionHistoryPage.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(historyPage.page()).isGreaterThanOrEqualTo(0);
        assertThat(historyPage.size()).isGreaterThan(0);
        assertThat(historyPage.totalItems()).isGreaterThanOrEqualTo(1);
        var item = historyPage.items().getFirst();
        assertThat(item.sessionDate()).isNotNull();
        assertThat(item.sessionType()).isIn(SessionType.FREE, SessionType.PROGRAM);
        assertThat(item.exerciseCount()).isGreaterThan(0);
    }

    private LoggedSessionDetail postFreeSession(LocalDate date) throws Exception {
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.FREE,
                null,
                date,
                "Quick session",
                null,
                1200,
                new SessionFeelingsInput(8, null),
                List.of(new ExerciseEntryInput(
                        null,
                        "Tire Flip",
                        "Tire Flip",
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

