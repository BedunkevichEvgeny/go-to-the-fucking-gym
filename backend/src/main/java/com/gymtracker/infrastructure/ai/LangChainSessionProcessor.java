package com.gymtracker.infrastructure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LangChainSessionProcessor {

    private static final Logger log = LoggerFactory.getLogger(LangChainSessionProcessor.class);

    public void process(SessionSummaryDto summary) {
        log.info("AI handoff placeholder processed session {} for user {} with {} exercise summaries",
                summary.sessionId(), summary.userId(), summary.exercises().size());
    }
}

