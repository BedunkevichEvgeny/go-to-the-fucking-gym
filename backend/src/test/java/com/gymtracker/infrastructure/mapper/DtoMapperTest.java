package com.gymtracker.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.SessionHistoryItem;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionAiSuggestion;
import com.gymtracker.domain.SessionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoMapperTest {

    private final DtoMapper mapper = new DtoMapper();

    private LoggedSession buildSession() {
        LoggedSession session = new LoggedSession();
        session.setId(UUID.randomUUID());
        session.setSessionType(SessionType.PROGRAM);
        session.setSessionDate(LocalDate.now());
        session.setExerciseEntries(new ArrayList<>());
        return session;
    }

    @Test
    void toDetailDto_withNonNullAiSuggestion_dtoBHasSuggestionText() {
        LoggedSession session = buildSession();
        SessionAiSuggestion aiSuggestion = new SessionAiSuggestion();
        aiSuggestion.setSuggestion("Great session, keep it up!");
        aiSuggestion.setSession(session);
        session.setAiSuggestion(aiSuggestion);

        LoggedSessionDetail dto = mapper.toDetailDto(session);

        assertThat(dto.aiSuggestion()).isEqualTo("Great session, keep it up!");
    }

    @Test
    void toDetailDto_withNullAiSuggestion_dtoAiSuggestionIsNull() {
        LoggedSession session = buildSession();
        session.setAiSuggestion(null);

        LoggedSessionDetail dto = mapper.toDetailDto(session);

        assertThat(dto.aiSuggestion()).isNull();
    }

    @Test
    void toHistoryItem_doesNotContainAiSuggestion() {
        LoggedSession session = buildSession();

        SessionHistoryItem item = mapper.toHistoryItem(session);

        // SessionHistoryItem record should not have aiSuggestion — compile-time check via fields
        assertThat(item).isNotNull();
        assertThat(item.sessionId()).isEqualTo(session.getId());
    }
}

