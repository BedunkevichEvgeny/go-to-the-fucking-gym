package com.gymtracker.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionType;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
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
class SessionHistoryServiceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoggedSessionRepository loggedSessionRepository;

    @Test
    void getHistoryList_itemsDoNotContainAiSuggestionField() throws Exception {
        UUID userId = UUID.randomUUID();
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setSessionType(SessionType.FREE);
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        loggedSessionRepository.save(session);

        mockMvc.perform(get("/api/logged-sessions")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sessionId").exists())
                .andExpect(jsonPath("$.items[0].aiSuggestion").doesNotExist());
    }
}

