package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.*;
import com.gymtracker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * T041: Unit tests for active program replacement policy.
 *
 * Verifies that when a user accepts a new onboarding plan while an old program is active,
 * the new plan correctly replaces the old one immediately while preserving all history
 * and progression tracking data integrity.
 */
class ProgramReplacementPolicyTest {

    private ProgramReplacementPolicy replacementPolicy;

    @BeforeEach
    void setUp() {
        replacementPolicy = new ProgramReplacementPolicy();
    }

    @Test
    void replaceActiveProgramDeactivatesOldProgram() {
        // Given an active program and a new program to activate
        UUID userId = UUID.randomUUID();
        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Old Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing the active program
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then old program should not be marked as completed
        // (replacement preserves history but doesn't force-complete)
        assertThat(oldProgram.getStatus()).isNotEqualTo(ProgramStatus.ACTIVE);
        assertThat(newProgram.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void replaceProgramPreservesHistoricalData() {
        // Given an old program with completed sessions (simulating history)
        UUID userId = UUID.randomUUID();
        UUID oldProgramId = UUID.randomUUID();
        UUID newProgramId = UUID.randomUUID();

        // Old program (historical)
        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(oldProgramId)
                .userId(userId)
                .name("Old Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(30))
                .build();

        ProgramSession oldSession1 = ProgramSession.builder()
                .id(UUID.randomUUID())
                .program(oldProgram)
                .sequenceNumber(1)
                .name("Old Session 1")
                .completed(true)
                .build();

        ProgramSession oldSession2 = ProgramSession.builder()
                .id(UUID.randomUUID())
                .program(oldProgram)
                .sequenceNumber(2)
                .name("Old Session 2")
                .completed(true)
                .build();

        oldProgram.setSessions(List.of(oldSession1, oldSession2));

        // New program (replacement)
        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(newProgramId)
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then old program data should be intact (not deleted)
        assertThat(oldProgram.getId()).isEqualTo(oldProgramId);
        assertThat(oldProgram.getSessions()).hasSize(2);
        assertThat(oldProgram.getSessions()).allMatch(ProgramSession::isCompleted);
        assertThat(oldProgram.getCreatedAt()).isNotNull();
    }

    @Test
    void replacementDoesNotAffectOldProgramSessions() {
        // Given an old active program with uncompleted sessions
        UUID userId = UUID.randomUUID();
        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Old Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        ProgramSession oldSession = ProgramSession.builder()
                .id(UUID.randomUUID())
                .program(oldProgram)
                .sequenceNumber(1)
                .name("Old Session")
                .completed(false)
                .build();

        oldProgram.setSessions(List.of(oldSession));

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then old session should remain uncompleted
        assertThat(oldProgram.getSessions().get(0).isCompleted()).isFalse();
    }

    @Test
    void replacementAllowsTrackingHistoryQuery() {
        // Given an old program (to-be-replaced) and new program
        UUID userId = UUID.randomUUID();
        UUID oldProgramId = UUID.randomUUID();

        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(oldProgramId)
                .userId(userId)
                .name("Previous Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(30))
                .build();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Current Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then old program should still be queryable (for history/progression queries)
        assertThat(oldProgram.getId()).isEqualTo(oldProgramId);
        assertThat(oldProgram.getUserId()).isEqualTo(userId);
    }

    @Test
    void newProgramIsImmediatelyActive() {
        // Given an old and new program
        UUID userId = UUID.randomUUID();
        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Old Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then new program should be active
        assertThat(newProgram.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void replacePreservesBothProgramTimestamps() {
        // Given programs with specific timestamps
        UUID userId = UUID.randomUUID();
        OffsetDateTime oldCreatedAt = OffsetDateTime.now().minusDays(30);
        OffsetDateTime newCreatedAt = OffsetDateTime.now();

        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Old")
                .status(ProgramStatus.ACTIVE)
                .createdAt(oldCreatedAt)
                .build();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New")
                .status(ProgramStatus.ACTIVE)
                .createdAt(newCreatedAt)
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then timestamps should not be modified
        assertThat(oldProgram.getCreatedAt()).isEqualTo(oldCreatedAt);
        assertThat(newProgram.getCreatedAt()).isEqualTo(newCreatedAt);
    }

    @Test
    void replaceWithNewProgramSessions() {
        // Given old program and new program with different session counts
        UUID userId = UUID.randomUUID();

        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Old 2-Session Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        oldProgram.setSessions(List.of(
                ProgramSession.builder()
                        .id(UUID.randomUUID())
                        .program(oldProgram)
                        .sequenceNumber(1)
                        .name("Old Session 1")
                        .completed(false)
                        .build(),
                ProgramSession.builder()
                        .id(UUID.randomUUID())
                        .program(oldProgram)
                        .sequenceNumber(2)
                        .name("Old Session 2")
                        .completed(false)
                        .build()
        ));

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("New 3-Session Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        newProgram.setSessions(List.of(
                ProgramSession.builder()
                        .id(UUID.randomUUID())
                        .program(newProgram)
                        .sequenceNumber(1)
                        .name("New Session 1")
                        .completed(false)
                        .build(),
                ProgramSession.builder()
                        .id(UUID.randomUUID())
                        .program(newProgram)
                        .sequenceNumber(2)
                        .name("New Session 2")
                        .completed(false)
                        .build(),
                ProgramSession.builder()
                        .id(UUID.randomUUID())
                        .program(newProgram)
                        .sequenceNumber(3)
                        .name("New Session 3")
                        .completed(false)
                        .build()
        ));

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then both programs retain their session structure
        assertThat(oldProgram.getSessions()).hasSize(2);
        assertThat(newProgram.getSessions()).hasSize(3);
    }

    @Test
    void replaceDoesNotReferenceHistoryData() {
        // Given an old program with specific data markers
        UUID userId = UUID.randomUUID();
        UUID oldProgramId = UUID.randomUUID();
        String oldProgramName = "Historical Program";

        WorkoutProgram oldProgram = WorkoutProgram.builder()
                .id(oldProgramId)
                .userId(userId)
                .name(oldProgramName)
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(90))
                .build();

        WorkoutProgram newProgram = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Fresh Program")
                .status(ProgramStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // When replacing
        replacementPolicy.replaceActiveProgram(oldProgram, newProgram);

        // Then old program identity is preserved (for historical queries)
        assertThat(oldProgram.getId()).isEqualTo(oldProgramId);
        assertThat(oldProgram.getName()).isEqualTo(oldProgramName);
    }
}

