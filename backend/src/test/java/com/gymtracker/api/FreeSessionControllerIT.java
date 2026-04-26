package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.SessionHistoryPage;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
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
class FreeSessionControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void postFreeSessionWithLibraryExerciseIdsReturnsCreated() throws Exception {
        Exercise bench = exerciseRepository.findByNameIgnoreCase("Bench Press").orElseThrow();
        LoggedSessionCreateRequest request = freeRequest(List.of(new ExerciseEntryInput(
                bench.getId(),
                null,
                "Bench Press",
                ExerciseType.STRENGTH,
                List.of(new StrengthSetInput(8, false, new BigDecimal("70"), WeightUnit.KG)),
                List.of())));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(detail.sessionType()).isEqualTo(SessionType.FREE);
        assertThat(detail.programSessionId()).isNull();
        assertThat(detail.exerciseEntries().getFirst().exerciseId()).isEqualTo(bench.getId());
    }

    @Test
    void postFreeSessionWithCustomExerciseNameReturnsCreated() throws Exception {
        LoggedSessionCreateRequest request = freeRequest(List.of(new ExerciseEntryInput(
                null,
                "Tire Flip",
                "Tire Flip",
                ExerciseType.BODYWEIGHT,
                List.of(new StrengthSetInput(12, true, null, null)),
                List.of())));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(detail.exerciseEntries().getFirst().customExerciseName()).isEqualTo("Tire Flip");
    }

    @Test
    void postFreeSessionWithMixedEntriesPersistsLibraryAndCustomData() throws Exception {
        Exercise bench = exerciseRepository.findByNameIgnoreCase("Bench Press").orElseThrow();
        LoggedSessionCreateRequest request = freeRequest(List.of(
                new ExerciseEntryInput(
                        bench.getId(),
                        null,
                        "Bench Press",
                        ExerciseType.STRENGTH,
                        List.of(new StrengthSetInput(8, false, new BigDecimal("70"), WeightUnit.KG)),
                        List.of()),
                new ExerciseEntryInput(
                        null,
                        "Tire Flip",
                        "Tire Flip",
                        ExerciseType.BODYWEIGHT,
                        List.of(new StrengthSetInput(12, true, null, null)),
                        List.of())));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(detail.exerciseEntries()).hasSize(2);
        assertThat(loggedSessionRepository.findById(detail.sessionId())).isPresent();
    }

    @Test
    void postFreeSessionWithCardioAndBodyweightDataPersistsCorrectly() throws Exception {
        LoggedSessionCreateRequest request = freeRequest(List.of(
                new ExerciseEntryInput(
                        null,
                        null,
                        "Running",
                        ExerciseType.CARDIO,
                        List.of(),
                        List.of(new CardioLapInput(900, new BigDecimal("3.2"), com.gymtracker.domain.DistanceUnit.KM))),
                new ExerciseEntryInput(
                        null,
                        "Pull Up",
                        "Pull Up",
                        ExerciseType.BODYWEIGHT,
                        List.of(new StrengthSetInput(10, true, null, null)),
                        List.of())));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(201);
        var cardio = detail.exerciseEntries().stream().filter(it -> it.exerciseType() == ExerciseType.CARDIO).findFirst().orElseThrow();
        var bodyweight = detail.exerciseEntries().stream().filter(it -> it.exerciseType() == ExerciseType.BODYWEIGHT).findFirst().orElseThrow();
        assertThat(cardio.cardioLaps()).hasSize(1);
        assertThat(bodyweight.sets().getFirst().weightValue()).isNull();
    }

    @Test
    void freeSessionDoesNotRequireProgramSessionAndDoesNotCompleteProgramSession() throws Exception {
        UUID programSessionId = firstProgramSessionId();
        boolean before = programSessionRepository.findById(programSessionId).orElseThrow().isCompleted();

        LoggedSessionCreateRequest request = freeRequest(List.of(new ExerciseEntryInput(
                null,
                "Tire Flip",
                "Tire Flip",
                ExerciseType.BODYWEIGHT,
                List.of(new StrengthSetInput(12, true, null, null)),
                List.of())));

        HttpResponse<String> response = postWithUser1(request);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(programSessionRepository.findById(programSessionId).orElseThrow().isCompleted()).isEqualTo(before);
    }

    @Test
    void createdFreeSessionAppearsInHistoryWithCorrectTypeAndExerciseCount() throws Exception {
        LoggedSessionCreateRequest request = freeRequest(List.of(
                new ExerciseEntryInput(
                        null,
                        "Tire Flip",
                        "Tire Flip",
                        ExerciseType.BODYWEIGHT,
                        List.of(new StrengthSetInput(12, true, null, null)),
                        List.of()),
                new ExerciseEntryInput(
                        null,
                        null,
                        "Running",
                        ExerciseType.CARDIO,
                        List.of(),
                        List.of(new CardioLapInput(600, null, null)))));

        HttpResponse<String> postResponse = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(postResponse.body(), LoggedSessionDetail.class);

        HttpResponse<String> historyResponse = getWithUser1("/api/logged-sessions/history?page=0&size=20");
        SessionHistoryPage history = objectMapper.readValue(historyResponse.body(), SessionHistoryPage.class);

        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(history.items().stream().anyMatch(item ->
                item.sessionId().equals(detail.sessionId())
                        && item.sessionType() == SessionType.FREE
                        && item.exerciseCount() == 2)).isTrue();
    }

    private UUID firstProgramSessionId() {
        return programSessionRepository
                .findByProgram_IdOrderBySequenceNumberAsc(activeProgramId())
                .getFirst()
                .getId();
    }

    private UUID activeProgramId() {
        return workoutProgramRepository
                .findFirstByUserIdAndStatus(UUID.fromString("11111111-1111-1111-1111-111111111111"), com.gymtracker.domain.ProgramStatus.ACTIVE)
                .orElseThrow()
                .getId();
    }

    private LoggedSessionCreateRequest freeRequest(List<ExerciseEntryInput> entries) {
        return new LoggedSessionCreateRequest(
                SessionType.FREE,
                null,
                LocalDate.of(2026, 4, 27),
                "Free workout",
                "Off-program",
                1800,
                new SessionFeelingsInput(8, "Felt strong"),
                entries);
    }

    private HttpResponse<String> postWithUser1(LoggedSessionCreateRequest request) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/logged-sessions")))
                .header("Authorization", basicAuth("user1", "password1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
        return HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithUser1(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(path)))
                .header("Authorization", basicAuth("user1", "password1"))
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

