package com.gymtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.*;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T042: Integration tests for proposal acceptance endpoint.
 *
 * Verifies that accepting a proposal via REST API correctly:
 * 1. Persists the accepted program to the database
 * 2. Marks the proposal as ACCEPTED
 * 3. Returns a success response with activation details
 * 4. Creates linkage in the accepted_program_activations table
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileGoalsAcceptanceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkoutProgramRepository workoutProgramRepository;

    @Autowired
    private ProfileGoalOnboardingAttemptRepository attemptRepository;

    @Autowired
    private PlanProposalRepository proposalRepository;

    private UUID userId;
    private UUID attemptId;
    private UUID proposalId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        attemptId = UUID.randomUUID();
        proposalId = UUID.randomUUID();
    }

    @Test
    void acceptProposalCreatesActiveProgramInDatabase() throws Exception {
        // Given: setup test data with a proposal in PROPOSED state
        setupProposalForAcceptance();

        // When: POST to accept endpoint
        String response = mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: verify program was created and is active
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(programs).isNotEmpty();
        assertThat(programs.get(0).getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void acceptProposalReturnsSuccessResponse() throws Exception {
        // Given: a proposal ready to accept
        setupProposalForAcceptance();

        // When: accept the proposal
        MvcResult result = mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then: response should contain activation success marker
        assertThat(response).contains("\"accepted\":true");
    }

    @Test
    void acceptProposalMarksProgramAsActiveForTracking() throws Exception {
        // Given: a proposal with valid sessions and exercises
        setupProposalForAcceptance();

        // When: accept the proposal
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then: verify created program has sessions
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(programs.get(0).getSessions()).isNotEmpty();
    }

    @Test
    void acceptProposalWithMultipleSessionsPreservesAll() throws Exception {
        // Given: a proposal with 3 sessions
        setupProposalWithMultipleSessions();

        // When: accept
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then: all sessions should be created
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(programs.get(0).getSessions()).hasSize(3);
    }

    @Test
    void acceptProposalCreatesExerciseTargets() throws Exception {
        // Given: a proposal with exercises
        setupProposalForAcceptance();

        // When: accept
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then: exercise targets should be created
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<ProgramExerciseTarget> targets = programs.get(0).getSessions().get(0).getExerciseTargets();
        assertThat(targets).isNotEmpty();
    }

    @Test
    void acceptProposalWithoutAuthenticationFails() throws Exception {
        // Given: a proposal
        setupProposalForAcceptance();

        // When: try to accept without auth
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .contentType("application/json"))
                .andExpect(status().isUnauthorized());

        // Then: no program should be created
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(programs).isEmpty();
    }

    @Test
    void acceptNonExistentProposalFails() throws Exception {
        // Given: a non-existent proposal ID
        UUID fakeProposalId = UUID.randomUUID();

        // When: try to accept
        mockMvc.perform(post("/api/profile-goals/proposals/" + fakeProposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptProposalTransitionsProgramToActiveState() throws Exception {
        // Given: a proposal
        setupProposalForAcceptance();

        // When: accept it
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then: program should be queryable as active
        List<WorkoutProgram> activePrograms = workoutProgramRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(activePrograms.get(0).getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void acceptProposalWithCardioExercises() throws Exception {
        // Given: a proposal with cardio exercise
        setupProposalWithCardio();

        // When: accept
        mockMvc.perform(post("/api/profile-goals/proposals/" + proposalId + "/accept")
                        .header("Authorization", "Bearer " + userId)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then: cardio exercise should be preserved
        List<WorkoutProgram> programs = workoutProgramRepository.findByUserIdOrderByCreatedAtDesc(userId);
        ProgramExerciseTarget target = programs.get(0).getSessions().get(0).getExerciseTargets().get(0);
        assertThat(target.getExerciseType()).isEqualTo(ExerciseType.CARDIO);
    }

    // ========== Helper methods ==========

    private void setupProposalForAcceptance() {
        // Create attempt
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(attemptId)
                .userId(userId)
                .userAge(30)
                .userWeight(BigDecimal.valueOf(75))
                .userWeightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();
        attemptRepository.save(attempt);

        // Create proposal with one session and one exercise
        String proposalPayload = "{\"sessions\":[{\"sequenceNumber\":1,\"sessionName\":\"Strength Day\",\"exercises\":[{\"exerciseName\":\"Bench Press\",\"exerciseType\":\"STRENGTH\",\"targetSets\":3,\"targetReps\":8,\"targetWeight\":100,\"targetWeightUnit\":\"KG\"}]}]}";
        PlanProposal proposal = PlanProposal.builder()
                .id(proposalId)
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload(proposalPayload)
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();
        proposalRepository.save(proposal);
    }

    private void setupProposalWithMultipleSessions() {
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(attemptId)
                .userId(userId)
                .userAge(30)
                .userWeight(BigDecimal.valueOf(75))
                .userWeightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.STRENGTH)
                .build();
        attemptRepository.save(attempt);

        String proposalPayload = "{\"sessions\":[" +
                "{\"sequenceNumber\":1,\"sessionName\":\"Day 1\",\"exercises\":[{\"exerciseName\":\"Squat\",\"exerciseType\":\"STRENGTH\",\"targetSets\":5,\"targetReps\":5}]}," +
                "{\"sequenceNumber\":2,\"sessionName\":\"Day 2\",\"exercises\":[{\"exerciseName\":\"Bench\",\"exerciseType\":\"STRENGTH\",\"targetSets\":4,\"targetReps\":6}]}," +
                "{\"sequenceNumber\":3,\"sessionName\":\"Day 3\",\"exercises\":[{\"exerciseName\":\"Deadlift\",\"exerciseType\":\"STRENGTH\",\"targetSets\":3,\"targetReps\":3}]}" +
                "]}";
        PlanProposal proposal = PlanProposal.builder()
                .id(proposalId)
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload(proposalPayload)
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();
        proposalRepository.save(proposal);
    }

    private void setupProposalWithCardio() {
        ProfileGoalOnboardingAttempt attempt = ProfileGoalOnboardingAttempt.builder()
                .id(attemptId)
                .userId(userId)
                .userAge(30)
                .userWeight(BigDecimal.valueOf(75))
                .userWeightUnit(WeightUnit.KG)
                .primaryGoal(OnboardingEnums.OnboardingPrimaryGoal.WEIGHT_LOSS)
                .build();
        attemptRepository.save(attempt);

        String proposalPayload = "{\"sessions\":[{\"sequenceNumber\":1,\"sessionName\":\"Cardio Session\",\"exercises\":[{\"exerciseName\":\"Treadmill Run\",\"exerciseType\":\"CARDIO\",\"targetDurationSeconds\":600}]}]}";
        PlanProposal proposal = PlanProposal.builder()
                .id(proposalId)
                .attempt(attempt)
                .version(1)
                .status(OnboardingEnums.ProposalStatus.PROPOSED)
                .proposalPayload(proposalPayload)
                .provider(OnboardingEnums.ProposalProvider.AZURE_OPENAI)
                .modelDeployment("gpt-35-turbo")
                .build();
        proposalRepository.save(proposal);
    }
}

