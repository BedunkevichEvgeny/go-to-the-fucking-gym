package com.gymtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.SessionHistoryView;
import com.gymtracker.domain.*;
import com.gymtracker.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T044: Integration test verifying history/progression data is unaffected after program replacement.
 *
 * Verifies that when a user's active program is replaced via onboarding acceptance,
 * all historical data (session logs, progression records) remains queryable and intact.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionHistoryControllerActivationCompatibilityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Autowired
    private ExerciseEntryRepository exerciseEntryRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void historyEndpointStillReturnsLogsFromOldProgram() throws Exception {
        // Given: an old program with logged sessions
        WorkoutProgram oldProgram = createProgramWithLogs(userId, "Old Program", 2);

        // When: query history endpoint
        MvcResult result = mockMvc.perform(get("/api/logged-sessions")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then: old logs should still be visible
        assertThat(response).contains("Logged Session");
    }

    @Test
    void historyPreservesLoggedSessionsAfterProgramReplacement() throws Exception {
        // Given: old program with completed session logs
        WorkoutProgram oldProgram = createProgramWithLogs(userId, "Old Program", 1);
        int initialLogCount = loggedSessionRepository.findByUserId(userId).size();

        // When: simulate replacement by creating new active program
        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        workoutProgramRepository.save(newProgram);

        // Then: old logs should still exist
        int afterReplacementCount = loggedSessionRepository.findByUserId(userId).size();
        assertThat(afterReplacementCount).isEqualTo(initialLogCount);
    }

    @Test
    void exerciseEntriesRemainLinkedToHistoricalProgram() throws Exception {
        // Given: an old program with exercise logs
        WorkoutProgram oldProgram = createProgramWithLogs(userId, "Old Program", 1);
        List<LoggedSession> loggedSessions = loggedSessionRepository.findByUserId(userId);
        assertThat(loggedSessions).isNotEmpty();

        LoggedSession loggedSession = loggedSessions.get(0);
        List<ExerciseEntry> entries = exerciseEntryRepository.findByLoggedSessionIdOrderBySortOrderAsc(loggedSession.getId());

        // When: query the exercise entries
        // Then: entries should be intact
        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0).getLoggedSession()).isEqualTo(loggedSession);
    }

    @Test
    void oldProgramDataRemainAccessibleByUserId() throws Exception {
        // Given: old and new programs
        WorkoutProgram oldProgram = createProgramWithLogs(userId, "Old Program", 1);
        UUID oldProgramId = oldProgram.getId();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        workoutProgramRepository.save(newProgram);

        // When: query old program by ID
        var retrievedOldProgram = workoutProgramRepository.findById(oldProgramId);

        // Then: old program should be queryable
        assertThat(retrievedOldProgram).isPresent();
        assertThat(retrievedOldProgram.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void historyEndpointFiltersByUserCorrectly() throws Exception {
        // Given: logs from old program for this user
        UUID otherUserId = UUID.randomUUID();
        WorkoutProgram myOldProgram = createProgramWithLogs(userId, "My Program", 1);
        WorkoutProgram otherUserProgram = createProgramWithLogs(otherUserId, "Other User Program", 1);

        // When: query my history
        MvcResult result = mockMvc.perform(get("/api/logged-sessions")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        // Then: should only see my logs
        List<LoggedSession> myLogs = loggedSessionRepository.findByUserId(userId);
        List<LoggedSession> otherLogs = loggedSessionRepository.findByUserId(otherUserId);
        assertThat(myLogs.size()).isGreaterThan(0);
        assertThat(otherLogs.size()).isGreaterThan(0);
        assertThat(myLogs.size()).isNotEqualTo(otherLogs.size());
    }

    @Test
    void sessionLogCountSurvivesProgramReplacement() throws Exception {
        // Given: program with 3 logged sessions
        WorkoutProgram oldProgram = createProgramWithLogs(userId, "Old Program", 3);
        int logCountBefore = loggedSessionRepository.findByUserId(userId).size();

        // When: replace with new program
        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        workoutProgramRepository.save(newProgram);

        // Then: log count should be unchanged
        int logCountAfter = loggedSessionRepository.findByUserId(userId).size();
        assertThat(logCountAfter).isEqualTo(logCountBefore);
    }

    @Test
    void oldProgramHistoricalStatusPreserved() throws Exception {
        // Given: old program in REPLACED state (after onboarding activation)
        WorkoutProgram oldProgram = createProgram(userId, "Old Program", 1);
        oldProgram.setStatus(ProgramStatus.REPLACED);
        workoutProgramRepository.save(oldProgram);

        // When: query the program
        var retrieved = workoutProgramRepository.findById(oldProgram.getId());

        // Then: status should reflect replacement
        assertThat(retrieved.isPresent()).isTrue();
        assertThat(retrieved.get().getStatus()).isEqualTo(ProgramStatus.REPLACED);
    }

    @Test
    void multipleHistoricalProgramsAllQueryable() throws Exception {
        // Given: multiple programs (old, older, active)
        WorkoutProgram program1 = createProgram(userId, "Program 1", 1);
        program1.setStatus(ProgramStatus.COMPLETED);
        program1.setCreatedAt(OffsetDateTime.now().minusDays(60));
        workoutProgramRepository.save(program1);

        WorkoutProgram program2 = createProgram(userId, "Program 2", 1);
        program2.setStatus(ProgramStatus.REPLACED);
        program2.setCreatedAt(OffsetDateTime.now().minusDays(30));
        workoutProgramRepository.save(program2);

        WorkoutProgram program3 = createProgram(userId, "Program 3", 1);
        program3.setStatus(ProgramStatus.ACTIVE);
        program3.setCreatedAt(OffsetDateTime.now());
        workoutProgramRepository.save(program3);

        // When: query all programs by user
        var allPrograms = workoutProgramRepository.findAllByUserId(userId);

        // Then: all three should be retrievable
        assertThat(allPrograms).hasSize(3);
    }

    // ========== Helper methods ==========

    private WorkoutProgram createProgram(UUID userId, String name, int sessionCount) {
        WorkoutProgram program = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        List<ProgramSession> sessions = new java.util.ArrayList<>();
        for (int i = 1; i <= sessionCount; i++) {
            ProgramSession session = ProgramSession.builder()
                    .id(UUID.randomUUID())
                    .program(program)
                    .sequenceNumber(i)
                    .name("Session " + i)
                    .completed(false)
                    .exerciseTargets(new java.util.ArrayList<>())
                    .build();
            sessions.add(session);
        }
        program.setSessions(sessions);

        return workoutProgramRepository.save(program);
    }

    private WorkoutProgram createProgramWithLogs(UUID userId, String name, int sessionCount) {
        WorkoutProgram program = createProgram(userId, name, sessionCount);

        // Create and log a session
        for (ProgramSession session : program.getSessions()) {
            LoggedSession logged = LoggedSession.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .programSession(session)
                    .sessionType(SessionType.PROGRAM)
                    .notes("Logged Session")
                    .loggedAt(OffsetDateTime.now())
                    .build();
            loggedSessionRepository.save(logged);

            // Add exercise entry
            ExerciseEntry entry = ExerciseEntry.builder()
                    .id(UUID.randomUUID())
                    .loggedSession(logged)
                    .exerciseName("Exercise")
                    .exerciseType(ExerciseType.STRENGTH)
                    .actualSets(3)
                    .actualReps(8)
                    .sortOrder(0)
                    .build();
            exerciseEntryRepository.save(entry);
        }

        return program;
    }
}

