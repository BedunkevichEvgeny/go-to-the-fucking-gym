package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgramSessionControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Test
    void getNextProgramSessionReturnsSessionWithTargets() {
        ResponseEntity<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"Upper Body\"");
        assertThat(response.getBody()).contains("\"exerciseName\":\"Bench Press\"");
        assertThat(response.getBody()).contains("\"exercises\"");
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenNoActiveProgram() {
        ResponseEntity<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getNextProgramSessionReturnsNoContentWhenProgramCompleted() {
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

        ResponseEntity<String> response = getWithBasicAuth("/api/program-sessions/next", "user1", "password1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getNextProgramSessionRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/program-sessions/next",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getNextProgramSessionEnforcesCrossUserIsolation() {
        ResponseEntity<String> response = getWithBasicAuth("/api/program-sessions/next", "user2", "password2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    private ResponseEntity<String> getWithBasicAuth(String path, String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth(username, password));
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

