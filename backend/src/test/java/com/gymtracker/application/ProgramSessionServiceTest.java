package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProgramSessionView;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramSessionServiceTest {

    @Mock
    private WorkoutProgramRepository workoutProgramRepository;

    @Mock
    private ProgramSessionRepository programSessionRepository;

    @Mock
    private ProgramExerciseTargetRepository targetRepository;

    @Mock
    private EntityManager entityManager;

    private ProgramSessionService programSessionService;

    @BeforeEach
    void setUp() {
        // Configure EntityManager mock to return empty results for queries
        TypedQuery<?> mockQuery = org.mockito.Mockito.mock(TypedQuery.class);
        when(mockQuery.setParameter(any(String.class), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(any(Integer.class))).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of());
        when(entityManager.createQuery(any(String.class), any(Class.class))).thenReturn((TypedQuery<?>) (Object) mockQuery);

        programSessionService = new ProgramSessionService(
                workoutProgramRepository,
                programSessionRepository,
                targetRepository,
                new DtoMapper(),
                entityManager);
    }

    @Test
    void loadNextUncompletedSessionReturnsTheNextSession() {
        UUID userId = UUID.randomUUID();
        WorkoutProgram program = WorkoutProgram.builder().id(UUID.randomUUID()).userId(userId).name("Test").status(ProgramStatus.ACTIVE).build();
        ProgramSession session = ProgramSession.builder().id(UUID.randomUUID()).program(program).sequenceNumber(1).name("Upper Body").completed(false).build();
        ProgramExerciseTarget target = ProgramExerciseTarget.builder().id(UUID.randomUUID()).programSession(session).exerciseName("Bench Press").exerciseType(ExerciseType.STRENGTH).targetSets(3).targetReps(8).sortOrder(0).build();

        when(workoutProgramRepository.findFirstByUserIdAndStatus(userId, ProgramStatus.ACTIVE)).thenReturn(Optional.of(program));
        when(programSessionRepository.findFirstByProgram_IdAndCompletedFalseOrderBySequenceNumberAsc(program.getId())).thenReturn(Optional.of(session));
        when(targetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getId())).thenReturn(List.of(target));

        Optional<ProgramSessionView> result = programSessionService.loadNextUncompletedSession(userId);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Upper Body");
        assertThat(result.get().exercises()).hasSize(1);
    }

    @Test
    void loadNextUncompletedSessionReturnsEmptyWhenProgramCompleted() {
        UUID userId = UUID.randomUUID();
        WorkoutProgram program = WorkoutProgram.builder().id(UUID.randomUUID()).userId(userId).name("Done").status(ProgramStatus.ACTIVE).build();

        when(workoutProgramRepository.findFirstByUserIdAndStatus(userId, ProgramStatus.ACTIVE)).thenReturn(Optional.of(program));
        when(programSessionRepository.findFirstByProgram_IdAndCompletedFalseOrderBySequenceNumberAsc(program.getId())).thenReturn(Optional.empty());

        assertThat(programSessionService.loadNextUncompletedSession(userId)).isEmpty();
    }

    @Test
    void loadNextUncompletedSessionEnforcesUserIsolation() {
        UUID userId = UUID.randomUUID();

        when(workoutProgramRepository.findFirstByUserIdAndStatus(userId, ProgramStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThat(programSessionService.loadNextUncompletedSession(userId)).isEmpty();
        verify(programSessionRepository, never()).findFirstByProgram_IdAndCompletedFalseOrderBySequenceNumberAsc(any());
    }

    @Test
    void markProgramSessionCompletedTransitionsToCompleted() {
        UUID userId = UUID.randomUUID();
        WorkoutProgram program = WorkoutProgram.builder().id(UUID.randomUUID()).userId(userId).name("Plan").status(ProgramStatus.ACTIVE).build();
        ProgramSession first = ProgramSession.builder().id(UUID.randomUUID()).program(program).sequenceNumber(1).name("S1").completed(false).build();
        ProgramSession second = ProgramSession.builder().id(UUID.randomUUID()).program(program).sequenceNumber(2).name("S2").completed(false).build();

        when(programSessionRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(programSessionRepository.findByProgram_IdOrderBySequenceNumberAsc(program.getId())).thenReturn(List.of(first, second));

        programSessionService.markProgramSessionCompleted(first.getId(), userId);

        assertThat(first.isCompleted()).isTrue();
        verify(programSessionRepository).save(first);
        verify(workoutProgramRepository, never()).save(program);
    }

    @Test
    void markProgramSessionCompletedThrowsForbiddenForWrongUser() {
        UUID owner = UUID.randomUUID();
        UUID anotherUser = UUID.randomUUID();
        WorkoutProgram program = WorkoutProgram.builder().id(UUID.randomUUID()).userId(owner).name("Owner Program").status(ProgramStatus.ACTIVE).build();
        ProgramSession session = ProgramSession.builder().id(UUID.randomUUID()).program(program).sequenceNumber(1).name("S1").completed(false).build();

        when(programSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> programSessionService.markProgramSessionCompleted(session.getId(), anotherUser))
                .isInstanceOf(ForbiddenException.class);
    }
}
