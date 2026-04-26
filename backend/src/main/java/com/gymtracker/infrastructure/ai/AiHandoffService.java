package com.gymtracker.infrastructure.ai;

import com.gymtracker.domain.LoggedSession;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiHandoffService {

    private static final Logger log = LoggerFactory.getLogger(AiHandoffService.class);

    private final LangChainSessionProcessor processor;

    public AiHandoffService(LangChainSessionProcessor processor) {
        this.processor = processor;
    }

    @Async("aiTaskExecutor")
    public void enqueueSessionForAiAnalysis(UUID userId, LoggedSession session) {
        try {
            processor.process(buildSessionSummary(userId, session));
        } catch (Exception exception) {
            log.error("AI handoff failed for session {}", session.getId(), exception);
        }
    }

    public SessionSummaryDto buildSessionSummary(UUID userId, LoggedSession session) {
        return new SessionSummaryDto(
                userId,
                session.getId(),
                session.getSessionType(),
                session.getSessionDate(),
                session.getTotalDurationSeconds(),
                session.getFeelings() == null ? null : session.getFeelings().getRating(),
                session.getFeelings() == null ? null : session.getFeelings().getComment(),
                session.getExerciseEntries().stream().map(entry -> entry.getExerciseNameSnapshot()).toList());
    }
}

