package com.gymtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ProgramSessionView;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T043: Integration test for 001 next-session compatibility after onboarding activation.
 *
 * Verifies that after accepting and activating a proposal as the active program,
 * the existing /program-sessions/next endpoint returns the first session from the
 * newly activated program without any contract or behavior regressions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProgramSessionControllerActivationCompatibilityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProgramSessionRepository programSessionRepository;

    @Autowired
    private ProgramExerciseTargetRepository targetRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void nextSessionReturnsFirstSessionFromActivatedProgram() throws Exception {
        // Given: a program activated from proposal
        WorkoutProgram program = createActivatedProgram(userId, "Onboarding Generated", 3);

        // When: call /program-sessions/next
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ProgramSessionView sessionView = objectMapper.readValue(response, ProgramSessionView.class);

        // Then: should return the first session
        assertThat(sessionView).isNotNull();
        assertThat(sessionView.programSessionId()).isNotNull();
        assertThat(sessionView.sequenceNumber()).isEqualTo(1);
    }

    @Test
    void nextSessionShowsSessionNameFromProposal() throws Exception {
        // Given: a program with specific session names
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 1);
        program.getSessions().get(0).setName("Strength Foundation");
        workoutProgramRepository.save(program);

        // When: get next session
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then: session name should be visible
        assertThat(response).contains("Strength Foundation");
    }

    @Test
    void nextSessionIncludesExerciseTargetsFromProposal() throws Exception {
        // Given: a program with exercises
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 1);
        ProgramSession session = program.getSessions().get(0);

        // Create exercise targets
        ProgramExerciseTarget target1 = ProgramExerciseTarget.builder()
                .id(UUID.randomUUID())
                .programSession(session)
                .exerciseName("Bench Press")
                .exerciseType(ExerciseType.STRENGTH)
                .targetSets(3)
                .targetReps(8)
                .targetWeight(BigDecimal.valueOf(100))
                .targetWeightUnit(WeightUnit.KG)
                .sortOrder(0)
                .build();

        ProgramExerciseTarget target2 = ProgramExerciseTarget.builder()
                .id(UUID.randomUUID())
                .programSession(session)
                .exerciseName("Incline Dumbbell Press")
                .exerciseType(ExerciseType.STRENGTH)
                .targetSets(3)
                .targetReps(10)
                .targetWeight(BigDecimal.valueOf(40))
                .targetWeightUnit(WeightUnit.KG)
                .sortOrder(1)
                .build();

        session.setExerciseTargets(List.of(target1, target2));
        targetRepository.saveAll(List.of(target1, target2));
        programSessionRepository.save(session);

        // When: get next session
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then: both exercises should be included
        assertThat(response).contains("Bench Press");
        assertThat(response).contains("Incline Dumbbell Press");
    }

    @Test
    void nextSessionSkipsCompletedSessionsFromActivatedProgram() throws Exception {
        // Given: a program with first session marked completed
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 3);
        program.getSessions().get(0).setCompleted(true);
        programSessionRepository.save(program.getSessions().get(0));

        // When: get next session
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ProgramSessionView sessionView = objectMapper.readValue(response, ProgramSessionView.class);

        // Then: should return second session (first was completed)
        assertThat(sessionView.sequenceNumber()).isEqualTo(2);
    }

    @Test
    void nextSessionReturnsNoContentWhenAllSessionsCompleted() throws Exception {
        // Given: a program with all sessions completed
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 2);
        for (ProgramSession session : program.getSessions()) {
            session.setCompleted(true);
        }
        programSessionRepository.saveAll(program.getSessions());

        // When: get next session
        mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                // Then: should return 204 No Content
                .andExpect(status().isNoContent());
    }

    @Test
    void nextSessionContractRemainsCompatibleWithLogging() throws Exception {
        // Given: an activated program
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 1);

        // When: get next session
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        // Then: response should be serializable to ProgramSessionView (001 contract)
        String response = result.getResponse().getContentAsString();
        assertThat(response).isNotEmpty();
        ProgramSessionView sessionView = objectMapper.readValue(response, ProgramSessionView.class);
        assertThat(sessionView.programSessionId()).isNotNull();
        assertThat(sessionView.sequenceNumber()).isGreaterThan(0);
    }

    @Test
    void nextSessionFromActivatedProgramIsLoggable() throws Exception {
        // Given: a program activated from proposal
        WorkoutProgram program = createActivatedProgram(userId, "AI Plan", 1);

        // When: get next session
        MvcResult result = mockMvc.perform(get("/api/program-sessions/next")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ProgramSessionView sessionView = objectMapper.readValue(response, ProgramSessionView.class);

        // Then: the session should have a valid programSessionId for logging
        assertThat(sessionView.programSessionId()).isNotNull();
        UUID sessionId = sessionView.programSessionId();

        // Verify it exists in the repository
        var session = programSessionRepository.findById(sessionId);
        assertThat(session).isPresent();
        assertThat(session.get().getProgram().getUserId()).isEqualTo(userId);
    }

    // ========== Helper methods ==========

    private WorkoutProgram createActivatedProgram(UUID userId, String name, int sessionCount) {
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
}

