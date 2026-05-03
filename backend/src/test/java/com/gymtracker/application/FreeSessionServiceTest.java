package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeSessionServiceTest {

    @Mock
    private LoggedSessionService loggedSessionService;

    private FreeSessionService freeSessionService;

    @BeforeEach
    void setUp() {
        freeSessionService = new FreeSessionService(loggedSessionService);
    }

    @Test
    void saveFreeSessionPersistsFreeSessionAndSupportsMixedEntries() {
        UUID userId = UUID.randomUUID();
        LoggedSessionCreateRequest request = freeRequest(List.of(
                new ExerciseEntryInput(
                        UUID.randomUUID(),
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
                        List.of()),
                new ExerciseEntryInput(
                        null,
                        null,
                        "Running",
                        ExerciseType.CARDIO,
                        List.of(),
                        List.of(new CardioLapInput(600, new BigDecimal("2.5"), com.gymtracker.domain.DistanceUnit.KM)))));

        LoggedSessionDetail detail = new LoggedSessionDetail(
                UUID.randomUUID(),
                SessionType.FREE,
                null,
                request.sessionDate(),
                request.name(),
                request.notes(),
                request.totalDurationSeconds(),
                request.feelings(),
                request.exerciseEntries().stream()
                        .map(entry -> new com.gymtracker.api.dto.ExerciseEntryView(
                                entry.exerciseId(),
                                entry.customExerciseName(),
                                entry.exerciseName(),
                                entry.exerciseType(),
                                List.of(),
                                List.of()))
                        .toList(),
                null);

        when(loggedSessionService.saveLoggedSession(userId, request)).thenReturn(detail);

        LoggedSessionDetail saved = freeSessionService.saveFreeSession(userId, request);

        assertThat(saved.sessionType()).isEqualTo(SessionType.FREE);
        assertThat(saved.programSessionId()).isNull();
        assertThat(saved.exerciseEntries()).hasSize(3);
        assertThat(saved.exerciseEntries().stream().anyMatch(entry -> entry.customExerciseName() != null)).isTrue();
        verify(loggedSessionService).saveLoggedSession(userId, request);
    }

    @Test
    void saveFreeSessionValidatesAtLeastOneExerciseRequired() {
        UUID userId = UUID.randomUUID();
        LoggedSessionCreateRequest request = freeRequest(List.of());
        when(loggedSessionService.saveLoggedSession(eq(userId), any())).thenThrow(new ValidationException("at least one exercise required"));

        assertThatThrownBy(() -> freeSessionService.saveFreeSession(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one exercise");
    }

    @Test
    void saveFreeSessionEnforcesFreeSessionType() {
        UUID userId = UUID.randomUUID();
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 27),
                "Program day",
                null,
                1800,
                new SessionFeelingsInput(7, "ok"),
                List.of(new ExerciseEntryInput(
                        null,
                        null,
                        "Bench Press",
                        ExerciseType.STRENGTH,
                        List.of(new StrengthSetInput(8, false, new BigDecimal("70"), WeightUnit.KG)),
                        List.of())));

        assertThatThrownBy(() -> freeSessionService.saveFreeSession(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("FREE sessions");
    }

    @Test
    void saveFreeSessionUsesAuthenticatedUserIdOnDelegation() {
        UUID authenticatedUser = UUID.randomUUID();
        LoggedSessionCreateRequest request = freeRequest(List.of(new ExerciseEntryInput(
                null,
                "Custom Push Up",
                "Custom Push Up",
                ExerciseType.BODYWEIGHT,
                List.of(new StrengthSetInput(10, true, null, null)),
                List.of())));

        freeSessionService.saveFreeSession(authenticatedUser, request);

        verify(loggedSessionService).saveLoggedSession(authenticatedUser, request);
    }

    private static LoggedSessionCreateRequest freeRequest(List<ExerciseEntryInput> entries) {
        return new LoggedSessionCreateRequest(
                SessionType.FREE,
                null,
                LocalDate.of(2026, 4, 27),
                "Saturday free session",
                "Mixed training",
                2400,
                new SessionFeelingsInput(8, "Felt strong"),
                entries);
    }
}

