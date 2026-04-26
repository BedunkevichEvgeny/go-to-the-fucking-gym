package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.SessionHistoryPage;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SessionHistoryServiceTest {

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    private SessionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SessionHistoryService(loggedSessionRepository, new DtoMapper());
    }

    @Test
    void getSessionHistoryReturnsReverseChronologicalOrderAndPageMetadata() {
        UUID userId = UUID.randomUUID();
        LoggedSession newest = sessionWithExercises(userId, LocalDate.of(2026, 4, 27), "Upper day", "Bench Press");
        LoggedSession older = sessionWithExercises(userId, LocalDate.of(2026, 4, 20), "Lower day", "Squat");

        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newest, older)));

        SessionHistoryPage result = service.getSessionHistory(userId, 0, 20, null, null, null);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(result.items().get(1).sessionDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalItems()).isEqualTo(2);
    }

    @Test
    void getSessionHistoryUsesRequestedPaginationValues() {
        UUID userId = UUID.randomUUID();
        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sessionWithExercises(userId, LocalDate.of(2026, 4, 27), "Day", "Bench"))));

        service.getSessionHistory(userId, 2, 10, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(loggedSessionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void getSessionHistoryCalculatesExerciseCountFromEntries() {
        UUID userId = UUID.randomUUID();
        LoggedSession session = sessionWithExercises(userId, LocalDate.of(2026, 4, 27), "Mixed", "Bench Press", "Running");
        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));

        SessionHistoryPage result = service.getSessionHistory(userId, 0, 20, null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().exerciseCount()).isEqualTo(2);
    }

    @Test
    void getSessionHistoryAppliesDateRangeFilterSpecification() {
        UUID userId = UUID.randomUUID();
        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getSessionHistory(userId, 0, 20, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), null);

        verify(loggedSessionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getSessionHistoryCombinesDateRangeAndExerciseNameFilters() {
        UUID userId = UUID.randomUUID();
        when(loggedSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getSessionHistory(userId, 1, 5, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "Bench Press");

        verify(loggedSessionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private static LoggedSession sessionWithExercises(UUID userId, LocalDate sessionDate, String name, String... exerciseNames) {
        LoggedSession session = LoggedSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionType(SessionType.FREE)
                .sessionDate(sessionDate)
                .name(name)
                .createdAt(OffsetDateTime.parse("2026-04-27T10:15:30Z"))
                .build();

        List<ExerciseEntry> entries = java.util.stream.IntStream.range(0, exerciseNames.length)
                .mapToObj(index -> ExerciseEntry.builder()
                        .id(UUID.randomUUID())
                        .loggedSession(session)
                        .exerciseNameSnapshot(exerciseNames[index])
                        .exerciseType(ExerciseType.STRENGTH)
                        .sortOrder(index)
                        .build())
                .toList();
        session.setExerciseEntries(entries);
        return session;
    }
}

