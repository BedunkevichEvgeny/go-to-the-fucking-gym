package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionFeelings;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.StrengthSet;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionDetailServiceTest {

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    private SessionDetailService service;

    @BeforeEach
    void setUp() {
        service = new SessionDetailService(loggedSessionRepository, new DtoMapper());
    }

    @Test
    void getSessionDetailsReturnsFullDetailsForOwnedSession() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = detailedSession(userId);
        when(loggedSessionRepository.findDetailedById(session.getId())).thenReturn(Optional.of(session));

        LoggedSessionDetail result = service.getSessionDetails(userId, session.getId());

        assertThat(result.sessionId()).isEqualTo(session.getId());
        assertThat(result.sessionType()).isEqualTo(SessionType.FREE);
        assertThat(result.feelings()).isNotNull();
        assertThat(result.feelings().rating()).isEqualTo(8);
        assertThat(result.exerciseEntries()).hasSize(1);
        assertThat(result.exerciseEntries().getFirst().sets()).hasSize(1);
    }

    @Test
    void getSessionDetailsThrowsForbiddenWhenSessionBelongsToAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        LoggedSession session = detailedSession(ownerId);
        when(loggedSessionRepository.findDetailedById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.getSessionDetails(intruderId, session.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getSessionDetailsThrowsNotFoundWhenSessionMissing() {
        UUID userId = UUID.randomUUID();
        UUID missingSessionId = UUID.randomUUID();
        when(loggedSessionRepository.findDetailedById(missingSessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessionDetails(userId, missingSessionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    private static LoggedSession detailedSession(UUID userId) {
        LoggedSession session = LoggedSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionType(SessionType.FREE)
                .sessionDate(LocalDate.of(2026, 4, 27))
                .name("Saturday workout")
                .notes("Strong day")
                .totalDurationSeconds(2400)
                .createdAt(OffsetDateTime.parse("2026-04-27T10:15:30Z"))
                .build();

        ExerciseEntry entry = ExerciseEntry.builder()
                .id(UUID.randomUUID())
                .loggedSession(session)
                .exerciseNameSnapshot("Bench Press")
                .exerciseType(ExerciseType.STRENGTH)
                .sortOrder(0)
                .build();

        StrengthSet set = StrengthSet.builder()
                .id(UUID.randomUUID())
                .exerciseEntry(entry)
                .setOrder(1)
                .reps(8)
                .weightValue(new BigDecimal("70"))
                .weightUnit(WeightUnit.KG)
                .bodyWeight(false)
                .build();
        entry.setStrengthSets(List.of(set));
        entry.setCardioLaps(List.of());
        session.setExerciseEntries(List.of(entry));

        SessionFeelings feelings = SessionFeelings.builder()
                .sessionId(session.getId())
                .session(session)
                .rating(8)
                .comment("Felt strong")
                .build();
        session.setFeelings(feelings);

        return session;
    }
}

