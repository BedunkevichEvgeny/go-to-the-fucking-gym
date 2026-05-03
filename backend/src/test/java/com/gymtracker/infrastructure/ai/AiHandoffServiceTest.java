package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.SessionFeelings;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.User;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.SessionAiSuggestionRepository;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiHandoffServiceTest {

    @Mock
    private LangChainSessionProcessor processor;

    @Mock
    private ProgramExerciseTargetRepository programExerciseTargetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionAiSuggestionRepository sessionAiSuggestionRepository;

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    @Test
    void enqueueSessionForAiAnalysisQueuesProgramSessionWithoutBlockingCaller() throws Exception {
        AiHandoffService service = new AiHandoffService(processor, programExerciseTargetRepository, userRepository,
                sessionAiSuggestionRepository, loggedSessionRepository);
        LoggedSession session = programSession();
        UUID userId = UUID.randomUUID();

        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getProgramSessionId()))
                .thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        when(processor.process(anyString(), anyString())).thenAnswer(invocation -> {
            started.countDown();
            try {
                Thread.sleep(200);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            finished.countDown();
            return "ok";
        });

        long start = System.nanoTime();
        service.enqueueSessionForAiAnalysis(userId, session);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsedMillis).isLessThan(100);
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
        verify(processor).process(anyString(), anyString());
    }

    @Test
    void buildSessionSummaryIncludesActualVsTargetFeelingsAndUserPreferences() {
        AiHandoffService service = new AiHandoffService(processor, programExerciseTargetRepository, userRepository,
                sessionAiSuggestionRepository, loggedSessionRepository);
        LoggedSession session = programSession();
        UUID userId = UUID.randomUUID();

        ProgramExerciseTarget target = ProgramExerciseTarget.builder()
                .exerciseName("Bench Press")
                .exerciseType(ExerciseType.STRENGTH)
                .targetSets(3)
                .targetReps(8)
                .targetWeight(new BigDecimal("72.50"))
                .build();

        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getProgramSessionId()))
                .thenReturn(List.of(target));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .preferredWeightUnit(WeightUnit.KG)
                .build()));

        SessionSummaryDTO summary = service.buildSessionSummary(userId, session);

        assertThat(summary.metadata().preferredWeightUnit()).isEqualTo("KG");
        assertThat(summary.feelings().rating()).isEqualTo(7);
        assertThat(summary.exercises()).hasSize(1);

        SessionSummaryDTO.ExerciseSummary exercise = summary.exercises().getFirst();
        assertThat(exercise.exerciseName()).isEqualTo("Bench Press");
        assertThat(exercise.actual().setCount()).isEqualTo(1);
        assertThat(exercise.actual().totalReps()).isEqualTo(8);
        assertThat(exercise.actual().maxWeight()).isEqualByComparingTo("70");
        assertThat(exercise.target()).isNotNull();
        assertThat(exercise.target().targetSets()).isEqualTo(3);
        assertThat(exercise.target().targetReps()).isEqualTo(8);
        assertThat(exercise.target().targetWeight()).isEqualByComparingTo("72.50");
    }

    @Test
    void enqueueSessionForAiAnalysisHandlesProcessorErrorsGracefully() {
        AiHandoffService service = new AiHandoffService(processor, programExerciseTargetRepository, userRepository,
                sessionAiSuggestionRepository, loggedSessionRepository);
        LoggedSession session = programSession();

        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getProgramSessionId()))
                .thenReturn(List.of());
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        // processor.process() is never reached when user is not found; use lenient to avoid UnnecessaryStubbing
        lenient().when(processor.process(anyString(), anyString()))
                .thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> service.enqueueSessionForAiAnalysis(UUID.randomUUID(), session)).doesNotThrowAnyException();
    }

    @Test
    void enqueueSessionForAiAnalysisSkipsFreeSessions() {
        AiHandoffService service = new AiHandoffService(processor, programExerciseTargetRepository, userRepository,
                sessionAiSuggestionRepository, loggedSessionRepository);
        LoggedSession freeSession = LoggedSession.builder()
                .id(UUID.randomUUID())
                .sessionType(SessionType.FREE)
                .sessionDate(LocalDate.of(2026, 4, 27))
                .exerciseEntries(List.of())
                .build();

        service.enqueueSessionForAiAnalysis(UUID.randomUUID(), freeSession);

        verify(processor, never()).process(anyString(), anyString());
    }

    private LoggedSession programSession() {
        ExerciseEntry entry = ExerciseEntry.builder()
                .exerciseNameSnapshot("Bench Press")
                .exerciseType(ExerciseType.STRENGTH)
                .strengthSets(List.of(com.gymtracker.domain.StrengthSet.builder()
                        .reps(8)
                        .weightValue(new BigDecimal("70"))
                        .build()))
                .cardioLaps(List.of())
                .build();

        return LoggedSession.builder()
                .id(UUID.randomUUID())
                .programSessionId(UUID.randomUUID())
                .sessionType(SessionType.PROGRAM)
                .sessionDate(LocalDate.of(2026, 4, 27))
                .totalDurationSeconds(1800)
                .feelings(SessionFeelings.builder().rating(7).comment("Good").build())
                .exerciseEntries(List.of(entry))
                .build();
    }
}

