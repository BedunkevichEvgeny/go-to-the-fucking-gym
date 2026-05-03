package com.gymtracker.application;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionAiSuggestion;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.ai.AiHandoffService;
import com.gymtracker.infrastructure.ai.LangChainSessionProcessor;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.SessionAiSuggestionRepository;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

    private AiHandoffService service;

    @BeforeEach
    void setUp() {
        service = new AiHandoffService(
                processor,
                programExerciseTargetRepository,
                userRepository,
                sessionAiSuggestionRepository,
                loggedSessionRepository);
    }

    private LoggedSession buildProgramSession() {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setSessionType(SessionType.PROGRAM);
        session.setProgramSessionId(UUID.randomUUID());
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        return session;
    }

    @Test
    void programSession_suggestionPersistedWhenAiReturnsNonBlankText() {
        LoggedSession session = buildProgramSession();
        when(processor.process(anyString(), anyString())).thenReturn("Great workout!");
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(any()))
                .thenReturn(new ArrayList<>());
        when(sessionAiSuggestionRepository.existsById(session.getId())).thenReturn(false);
        when(loggedSessionRepository.getReferenceById(session.getId())).thenReturn(session);

        service.enqueueSessionForAiAnalysis(UUID.randomUUID(), session);

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(sessionAiSuggestionRepository).save(any(SessionAiSuggestion.class)));
    }

    @Test
    void programSession_blankAiResponse_saveNeverCalled() {
        LoggedSession session = buildProgramSession();
        when(processor.process(anyString(), anyString())).thenReturn("   ");
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(any()))
                .thenReturn(new ArrayList<>());

        service.enqueueSessionForAiAnalysis(UUID.randomUUID(), session);

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(sessionAiSuggestionRepository, never()).save(any()));
    }

    @Test
    void programSession_existsByIdTrue_saveNeverCalled() {
        LoggedSession session = buildProgramSession();
        when(processor.process(anyString(), anyString())).thenReturn("Great workout!");
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(any()))
                .thenReturn(new ArrayList<>());
        when(sessionAiSuggestionRepository.existsById(session.getId())).thenReturn(true);

        service.enqueueSessionForAiAnalysis(UUID.randomUUID(), session);

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(sessionAiSuggestionRepository, never()).save(any()));
    }

    @Test
    void freeSession_noRepositoryInteraction() {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setSessionType(SessionType.FREE);
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());

        service.enqueueSessionForAiAnalysis(UUID.randomUUID(), session);

        verify(sessionAiSuggestionRepository, never()).existsById(any());
        verify(sessionAiSuggestionRepository, never()).save(any());
        verify(processor, never()).process(anyString(), anyString());
    }
}

