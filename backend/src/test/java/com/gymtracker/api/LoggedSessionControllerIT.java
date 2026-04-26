package com.gymtracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
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
class LoggedSessionControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Test
    void postLoggedSessionCreatesProgramSessionLogAndReturnsSessionId() throws Exception {
        UUID session1Id = firstProgramSessionId();
        LoggedSessionCreateRequest request = session1Request(session1Id, LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(detail.sessionId()).isNotNull();
        assertThat(detail.sessionType()).isEqualTo(SessionType.PROGRAM);
    }

    @Test
    void postLoggedSessionPersistsEntriesSetsAndFeelings() throws Exception {
        UUID session1Id = firstProgramSessionId();
        LoggedSessionCreateRequest request = session1Request(session1Id, LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        UUID loggedId = detail.sessionId();
        assertThat(loggedSessionRepository.findById(loggedId)).isPresent();
        assertThat(detail.exerciseEntries()).hasSize(3);
        assertThat(detail.feelings()).isNotNull();
        assertThat(detail.feelings().rating()).isEqualTo(7);
        assertThat(detail.feelings().comment()).isEqualTo("Felt solid");
    }

    @Test
    void postLoggedSessionWithZeroSetsReturnsBadRequest() throws Exception {
        UUID session1Id = firstProgramSessionId();
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                session1Id,
                LocalDate.of(2026, 4, 27),
                null,
                null,
                1800,
                new SessionFeelingsInput(7, null),
                List.of(new ExerciseEntryInput(null, null, "Bench Press", ExerciseType.STRENGTH, List.of(), List.of())));

        HttpResponse<String> response = postWithUser1(request);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void postLoggedSessionRejectsAlreadyCompletedProgramSession() throws Exception {
        UUID session1Id = firstProgramSessionId();
        var session = programSessionRepository.findById(session1Id).orElseThrow();
        session.setCompleted(true);
        programSessionRepository.save(session);

        LoggedSessionCreateRequest request = session1Request(session1Id, LocalDate.of(2026, 4, 27));
        HttpResponse<String> response = postWithUser1(request);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void postLoggedSessionPersistsBodyweightSetWithoutWeight() throws Exception {
        UUID session1Id = firstProgramSessionId();
        LoggedSessionCreateRequest request = session1Request(session1Id, LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = postWithUser1(request);
        LoggedSessionDetail detail = objectMapper.readValue(response.body(), LoggedSessionDetail.class);

        var pullUp = detail.exerciseEntries().stream()
                .filter(entry -> entry.exerciseName().equals("Pull Up"))
                .findFirst()
                .orElseThrow();
        assertThat(pullUp.sets()).hasSize(1);
        assertThat(pullUp.sets().getFirst().isBodyWeight()).isTrue();
        assertThat(pullUp.sets().getFirst().weightValue()).isNull();
    }

    @Test
    void postLoggedSessionMarksProgramSessionCompleted() throws Exception {
        UUID session1Id = firstProgramSessionId();
        LoggedSessionCreateRequest request = session1Request(session1Id, LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = postWithUser1(request);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(programSessionRepository.findById(session1Id).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    void postLoggedSessionWithWrongNextSessionReturnsForbidden() throws Exception {
        UUID secondSessionId = secondProgramSessionId();
        LoggedSessionCreateRequest request = session2Request(secondSessionId, LocalDate.of(2026, 4, 27));

        HttpResponse<String> response = postWithUser1(request);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void completingFinalProgramSessionTransitionsProgramToCompleted() throws Exception {
        UUID session1Id = firstProgramSessionId();
        UUID session2Id = secondProgramSessionId();

        HttpResponse<String> firstResponse = postWithUser1(session1Request(session1Id, LocalDate.of(2026, 4, 27)));
        HttpResponse<String> secondResponse = postWithUser1(session2Request(session2Id, LocalDate.of(2026, 4, 28)));

        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(secondResponse.statusCode()).isEqualTo(201);
        var program = workoutProgramRepository.findFirstByUserIdAndStatus(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        ProgramStatus.COMPLETED)
                .orElseThrow();
        assertThat(program.getCompletedAt()).isNotNull();
    }

    private UUID firstProgramSessionId() {
        return programSessionRepository
                .findByProgram_IdOrderBySequenceNumberAsc(activeProgramId())
                .getFirst()
                .getId();
    }

    private UUID secondProgramSessionId() {
        return programSessionRepository
                .findByProgram_IdOrderBySequenceNumberAsc(activeProgramId())
                .get(1)
                .getId();
    }

    private UUID activeProgramId() {
        return workoutProgramRepository
                .findFirstByUserIdAndStatus(UUID.fromString("11111111-1111-1111-1111-111111111111"), ProgramStatus.ACTIVE)
                .orElseThrow()
                .getId();
    }

    private LoggedSessionCreateRequest session1Request(UUID programSessionId, LocalDate date) {
        return new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                programSessionId,
                date,
                null,
                null,
                1800,
                new SessionFeelingsInput(7, "Felt solid"),
                List.of(
                        new ExerciseEntryInput(null, null, "Bench Press", ExerciseType.STRENGTH,
                                List.of(new StrengthSetInput(8, false, new BigDecimal("70"), WeightUnit.KG)), List.of()),
                        new ExerciseEntryInput(null, null, "Pull Up", ExerciseType.BODYWEIGHT,
                                List.of(new StrengthSetInput(8, true, null, null)), List.of()),
                        new ExerciseEntryInput(null, null, "Running", ExerciseType.CARDIO,
                                List.of(), List.of(new CardioLapInput(900, null, null)))));
    }

    private LoggedSessionCreateRequest session2Request(UUID programSessionId, LocalDate date) {
        return new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                programSessionId,
                date,
                null,
                null,
                1700,
                new SessionFeelingsInput(8, "Heavy day"),
                List.of(
                        new ExerciseEntryInput(null, null, "Back Squat", ExerciseType.STRENGTH,
                                List.of(new StrengthSetInput(6, false, new BigDecimal("100"), WeightUnit.KG)), List.of()),
                        new ExerciseEntryInput(null, null, "Deadlift", ExerciseType.STRENGTH,
                                List.of(new StrengthSetInput(5, false, new BigDecimal("120"), WeightUnit.KG)), List.of())));
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

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}


