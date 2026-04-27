package com.gymtracker.application;

import com.gymtracker.api.dto.SessionHistoryPage;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.SessionSpecifications;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SessionHistoryService.class);

    private final LoggedSessionRepository loggedSessionRepository;
    private final DtoMapper dtoMapper;

    public SessionHistoryService(LoggedSessionRepository loggedSessionRepository, DtoMapper dtoMapper) {
        this.loggedSessionRepository = loggedSessionRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public SessionHistoryPage getSessionHistory(UUID userId, int page, int size, LocalDate dateFrom, LocalDate dateTo, String exerciseName) {
        Specification<LoggedSession> specification = Specification.where(SessionSpecifications.forUser(userId))
                .and(SessionSpecifications.dateFrom(dateFrom))
                .and(SessionSpecifications.dateTo(dateTo))
                .and(SessionSpecifications.exerciseNameContains(exerciseName));
        var resultPage = loggedSessionRepository.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sessionDate", "createdAt")));
        log.info("Loaded {} history entries for user {}", resultPage.getNumberOfElements(), userId);
        return new SessionHistoryPage(
                resultPage.getContent().stream().map(dtoMapper::toHistoryItem).toList(),
                page,
                size,
                resultPage.getTotalElements());
    }
}


