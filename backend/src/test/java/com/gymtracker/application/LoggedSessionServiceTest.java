package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.ProgramSessionView;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.AiHandoffService;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoggedSessionServiceTest {

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    @Mock
    private ProgramSessionService programSessionService;

    @Mock
    private SessionValidatorService sessionValidatorService;

    @Mock
    private ExerciseLibraryService exerciseLibraryService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private AiHandoffService aiHandoffService;

    @Captor
    private ArgumentCaptor<LoggedSession> loggedSessionCaptor;

    private LoggedSessionService service;

    @BeforeEach
    void setUp() {
        service = new LoggedSessionService(
                loggedSessionRepository,
                programSessionService,
                sessionValidatorService,
                exerciseLibraryService,
                dtoMapper,
                aiHandoffService);
    }

    @Test
    void saveLoggedSessionPersistsSessionWithEntriesAndSets() {
        UUID userId = UUID.randomUUID();
        UUID programSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(programSessionId, List.of(
                strengthEntry("Bench Press", false, new BigDecimal("70")),
                cardioEntry("Running", 900)));

        when(programSessionService.loadNextUncompletedSession(userId))
                .thenReturn(Optional.of(new ProgramSessionView(programSessionId, 1, "Upper Body", List.of())));
        when(loggedSessionRepository.save(any(LoggedSession.class))).thenAnswer(invocation -> {
            LoggedSession saved = invocation.getArgument(0, LoggedSession.class);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(dtoMapper.toDetailDto(any(LoggedSession.class))).thenReturn(new LoggedSessionDetail(
                UUID.randomUUID(),
                SessionType.PROGRAM,
                programSessionId,
                request.sessionDate(),
                null,
                null,
                null,
                request.feelings(),
                List.of(),
                null));

        service.saveLoggedSession(userId, request);

        verify(loggedSessionRepository).save(loggedSessionCaptor.capture());
        LoggedSession saved = loggedSessionCaptor.getValue();
        assertThat(saved.getExerciseEntries()).hasSize(2);
        assertThat(saved.getExerciseEntries().getFirst().getStrengthSets()).hasSize(1);
        assertThat(saved.getExerciseEntries().getLast().getCardioLaps()).hasSize(1);
        assertThat(saved.getFeelings().getRating()).isEqualTo(7);
        verify(programSessionService).markProgramSessionCompleted(programSessionId, userId);
        verify(aiHandoffService).enqueueSessionForAiAnalysis(userId, saved);
    }

    @Test
    void saveLoggedSessionValidatesProgramSessionIdMatchesNextUncompleted() {
        UUID userId = UUID.randomUUID();
        UUID expectedProgramSessionId = UUID.randomUUID();
        UUID submittedProgramSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(submittedProgramSessionId, List.of(strengthEntry("Bench Press", false, new BigDecimal("70"))));

        when(programSessionService.loadNextUncompletedSession(userId))
                .thenReturn(Optional.of(new ProgramSessionView(expectedProgramSessionId, 1, "Upper Body", List.of())));

        assertThatThrownBy(() -> service.saveLoggedSession(userId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the next uncompleted program session can be logged");
    }

    @Test
    void saveLoggedSessionSavesSessionFeelingsWithOptionalComment() {
        UUID userId = UUID.randomUUID();
        UUID programSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                programSessionId,
                LocalDate.of(2026, 4, 27),
                null,
                null,
                null,
                new SessionFeelingsInput(9, null),
                List.of(strengthEntry("Pull Up", true, null)));

        when(programSessionService.loadNextUncompletedSession(userId))
                .thenReturn(Optional.of(new ProgramSessionView(programSessionId, 1, "Upper Body", List.of())));
        when(loggedSessionRepository.save(any(LoggedSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dtoMapper.toDetailDto(any(LoggedSession.class))).thenReturn(new LoggedSessionDetail(
                UUID.randomUUID(),
                SessionType.PROGRAM,
                programSessionId,
                request.sessionDate(),
                null,
                null,
                null,
                request.feelings(),
                List.of(),
                null));

        service.saveLoggedSession(userId, request);

        verify(loggedSessionRepository).save(loggedSessionCaptor.capture());
        assertThat(loggedSessionCaptor.getValue().getFeelings().getRating()).isEqualTo(9);
        assertThat(loggedSessionCaptor.getValue().getFeelings().getComment()).isNull();
    }

    @Test
    void saveLoggedSessionRejectsEmptyExerciseList() {
        UUID userId = UUID.randomUUID();
        UUID programSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(programSessionId, List.of());

        assertThatThrownBy(() -> service.saveLoggedSession(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one exercise");
        verify(loggedSessionRepository, never()).save(any());
    }

    @Test
    void saveLoggedSessionRejectsExerciseWithZeroSets() {
        UUID userId = UUID.randomUUID();
        UUID programSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(programSessionId, List.of(
                new ExerciseEntryInput(null, null, "Bench Press", ExerciseType.STRENGTH, List.of(), List.of())));

        // Simulate real validator behavior for zero sets.
        org.mockito.Mockito.doThrow(new ValidationException("Strength and bodyweight exercises require at least one set"))
                .when(sessionValidatorService).validateExerciseEntry(any(ExerciseEntryInput.class));

        assertThatThrownBy(() -> service.saveLoggedSession(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one set");
    }

    @Test
    void saveLoggedSessionAcceptsBodyweightSetWithoutWeight() {
        UUID userId = UUID.randomUUID();
        UUID programSessionId = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(programSessionId, List.of(strengthEntry("Pull Up", true, null)));

        when(programSessionService.loadNextUncompletedSession(userId))
                .thenReturn(Optional.of(new ProgramSessionView(programSessionId, 1, "Upper Body", List.of())));
        when(loggedSessionRepository.save(any(LoggedSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dtoMapper.toDetailDto(any(LoggedSession.class))).thenReturn(new LoggedSessionDetail(
                UUID.randomUUID(),
                SessionType.PROGRAM,
                programSessionId,
                request.sessionDate(),
                null,
                null,
                null,
                request.feelings(),
                List.of(),
                null));

        service.saveLoggedSession(userId, request);

        verify(loggedSessionRepository).save(loggedSessionCaptor.capture());
        assertThat(loggedSessionCaptor.getValue().getExerciseEntries().getFirst().getStrengthSets().getFirst().getWeightValue()).isNull();
        assertThat(loggedSessionCaptor.getValue().getExerciseEntries().getFirst().getStrengthSets().getFirst().isBodyWeight()).isTrue();
    }

    @Test
    void saveLoggedSessionEnforcesUserOwnership() {
        UUID userId = UUID.randomUUID();
        UUID anotherUsersNextSession = UUID.randomUUID();
        UUID submittedSession = UUID.randomUUID();
        LoggedSessionCreateRequest request = validProgramRequest(submittedSession, List.of(strengthEntry("Bench Press", false, new BigDecimal("70"))));

        when(programSessionService.loadNextUncompletedSession(userId))
                .thenReturn(Optional.of(new ProgramSessionView(anotherUsersNextSession, 1, "Other", List.of())));

        assertThatThrownBy(() -> service.saveLoggedSession(userId, request))
                .isInstanceOf(ForbiddenException.class);
    }

    private static LoggedSessionCreateRequest validProgramRequest(UUID programSessionId, List<ExerciseEntryInput> entries) {
        return new LoggedSessionCreateRequest(
                SessionType.PROGRAM,
                programSessionId,
                LocalDate.of(2026, 4, 27),
                null,
                null,
                3600,
                new SessionFeelingsInput(7, "Felt good"),
                entries);
    }

    private static ExerciseEntryInput strengthEntry(String name, boolean bodyweight, BigDecimal weight) {
        return new ExerciseEntryInput(
                null,
                null,
                name,
                bodyweight ? ExerciseType.BODYWEIGHT : ExerciseType.STRENGTH,
                List.of(new StrengthSetInput(8, bodyweight, weight, bodyweight ? null : WeightUnit.KG)),
                List.of());
    }

    private static ExerciseEntryInput cardioEntry(String name, int durationSeconds) {
        return new ExerciseEntryInput(
                null,
                null,
                name,
                ExerciseType.CARDIO,
                List.of(),
                List.of(new CardioLapInput(durationSeconds, null, null)));
    }
}


