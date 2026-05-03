package com.gymtracker.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.ai.AiHandoffService;
import com.gymtracker.infrastructure.ai.LangChainSessionProcessor;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.SessionAiSuggestionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoggedSessionServiceIT {

    @Autowired
    private AiHandoffService aiHandoffService;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Autowired
    private SessionAiSuggestionRepository sessionAiSuggestionRepository;

    @MockitoBean
    private LangChainSessionProcessor langChainSessionProcessor;

    private LoggedSession saveProgramSession() {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setUserId(UUID.randomUUID());
        session.setSessionType(SessionType.PROGRAM);
        session.setProgramSessionId(UUID.randomUUID());
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        return loggedSessionRepository.save(session);
    }

    @Test
    void saveProgramSession_suggestionRowCreatedAfterAsyncStep() {
        when(langChainSessionProcessor.process(anyString(), anyString())).thenReturn("Keep pushing!");
        LoggedSession session = saveProgramSession();

        aiHandoffService.enqueueSessionForAiAnalysis(session.getUserId(), session);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            assertThat(sessionAiSuggestionRepository.existsById(session.getId())).isTrue();
            assertThat(sessionAiSuggestionRepository.findById(session.getId()))
                    .isPresent()
                    .hasValueSatisfying(s -> assertThat(s.getSuggestion()).isNotBlank());
        });
    }

    @Test
    void saveFreeSession_noSuggestionRowCreated() {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setUserId(UUID.randomUUID());
        session.setSessionType(SessionType.FREE);
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        loggedSessionRepository.save(session);

        aiHandoffService.enqueueSessionForAiAnalysis(session.getUserId(), session);

        assertThat(sessionAiSuggestionRepository.existsById(session.getId())).isFalse();
    }

    @Test
    void saveProgramSession_immutabilityGuard_suggestionNotOverwritten() {
        when(langChainSessionProcessor.process(anyString(), anyString()))
                .thenReturn("First suggestion.")
                .thenReturn("Second suggestion.");
        LoggedSession session = saveProgramSession();

        aiHandoffService.enqueueSessionForAiAnalysis(session.getUserId(), session);

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(sessionAiSuggestionRepository.existsById(session.getId())).isTrue());

        String firstSuggestion = sessionAiSuggestionRepository.findById(session.getId())
                .map(s -> s.getSuggestion()).orElse(null);

        // Trigger second persistence attempt
        aiHandoffService.enqueueSessionForAiAnalysis(session.getUserId(), session);

        await().atMost(3, SECONDS).untilAsserted(() -> {
            String currentSuggestion = sessionAiSuggestionRepository.findById(session.getId())
                    .map(s -> s.getSuggestion()).orElse(null);
            assertThat(currentSuggestion).isEqualTo(firstSuggestion);
        });
    }
}

