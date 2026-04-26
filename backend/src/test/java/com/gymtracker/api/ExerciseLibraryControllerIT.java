package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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
class ExerciseLibraryControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void getExercisesWithQueryReturnsMatchingNames() throws Exception {
        HttpResponse<String> response = getWithUser1("/api/exercises?query=bench");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload.isArray()).isTrue();
        assertThat(payload.toString().toLowerCase()).contains("bench press");
    }

    @Test
    void getExercisesWithoutQueryReturnsTopExercisesOrderedByUsage() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Exercise bench = exerciseRepository.findByNameIgnoreCase("Bench Press").orElseThrow();
        Exercise deadlift = exerciseRepository.findByNameIgnoreCase("Deadlift").orElseThrow();

        persistUsage(userId, bench, 3);
        persistUsage(userId, deadlift, 1);

        HttpResponse<String> response = getWithUser1("/api/exercises");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload.size()).isLessThanOrEqualTo(50);

        List<String> names = new ArrayList<>();
        payload.forEach(node -> names.add(node.get("name").asText()));
        assertThat(names.indexOf("Bench Press")).isLessThan(names.indexOf("Deadlift"));
    }

    @Test
    void getExercisesResponseIncludesCoreFields() throws Exception {
        HttpResponse<String> response = getWithUser1("/api/exercises?query=bench");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload.get(0).hasNonNull("name")).isTrue();
        assertThat(payload.get(0).hasNonNull("category")).isTrue();
        assertThat(payload.get(0).hasNonNull("type")).isTrue();
        assertThat(payload.get(0).has("description")).isTrue();
    }

    @Test
    void getExercisesReturnsActiveExercisesOnly() throws Exception {
        exerciseRepository.save(Exercise.builder()
                .name("Secret Curl")
                .category("Arms")
                .type(com.gymtracker.domain.ExerciseType.STRENGTH)
                .description("Inactive test exercise")
                .active(false)
                .build());

        HttpResponse<String> response = getWithUser1("/api/exercises?query=secret");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload).isEmpty();
    }

    private HttpResponse<String> getWithUser1(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
                .header("Authorization", basicAuth("user1", "password1"))
                .GET()
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void persistUsage(UUID userId, Exercise exercise, int count) {
        for (int i = 0; i < count; i++) {
            LoggedSession session = LoggedSession.builder()
                    .userId(userId)
                    .sessionType(SessionType.FREE)
                    .sessionDate(LocalDate.of(2026, 4, 20).plusDays(i))
                    .exerciseEntries(new ArrayList<>())
                    .build();

            ExerciseEntry entry = ExerciseEntry.builder()
                    .loggedSession(session)
                    .exerciseId(exercise.getId())
                    .exerciseNameSnapshot(exercise.getName())
                    .exerciseType(exercise.getType())
                    .sortOrder(0)
                    .build();

            session.setExerciseEntries(List.of(entry));
            loggedSessionRepository.save(session);
        }
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}

