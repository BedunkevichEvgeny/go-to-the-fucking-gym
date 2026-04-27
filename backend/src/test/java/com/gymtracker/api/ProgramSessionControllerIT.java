package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgramSessionControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Test
    void getNextProgramSessionReturnsSessionWithTargets() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Upper Body\"");
        assertThat(response.body()).contains("\"exerciseName\":\"Bench Press\"");
        assertThat(response.body()).contains("\"exercises\"");
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenNoActiveProgram() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenProgramCompleted() throws Exception {
        var program = workoutProgramRepository
                .findFirstByUserIdAndStatus(
                        java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        ProgramStatus.ACTIVE)
                .orElseThrow();
        var sessions = programSessionRepository.findByProgram_IdOrderBySequenceNumberAsc(program.getId());
        sessions.forEach(session -> session.setCompleted(true));
        programSessionRepository.saveAll(sessions);
        program.setStatus(ProgramStatus.COMPLETED);
        program.setCompletedAt(OffsetDateTime.now());
        workoutProgramRepository.save(program);

        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void getNextProgramSessionRequiresAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/program-sessions/next")))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void getNextProgramSessionEnforcesCrossUserIsolation() throws Exception {
        HttpResponse<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> getWithBasicAuth(String path, String username, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
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
