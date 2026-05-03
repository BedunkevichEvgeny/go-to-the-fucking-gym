package com.gymtracker.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionAiSuggestion;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.SessionAiSuggestionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionDetailServiceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Autowired
    private SessionAiSuggestionRepository sessionAiSuggestionRepository;

    private LoggedSession savedProgramSession(UUID userId) {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setSessionType(SessionType.PROGRAM);
        session.setProgramSessionId(UUID.randomUUID());
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        return loggedSessionRepository.save(session);
    }

    @Test
    void getSessionDetail_withSuggestionInDb_responseContainsAiSuggestion() throws Exception {
        UUID userId = UUID.randomUUID();
        LoggedSession session = savedProgramSession(userId);

        SessionAiSuggestion suggestion = new SessionAiSuggestion();
        suggestion.setSession(session);
        suggestion.setSuggestion("Excellent form today!");
        sessionAiSuggestionRepository.save(suggestion);

        mockMvc.perform(get("/api/logged-sessions/{id}", session.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiSuggestion", is("Excellent form today!")));
    }

    @Test
    void getSessionDetail_withoutSuggestionRow_responseAiSuggestionIsNull() throws Exception {
        UUID userId = UUID.randomUUID();
        LoggedSession session = savedProgramSession(userId);

        mockMvc.perform(get("/api/logged-sessions/{id}", session.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiSuggestion", nullValue()));
    }
}

