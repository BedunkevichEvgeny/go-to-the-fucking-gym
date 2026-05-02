package com.gymtracker.application;

import com.gymtracker.api.dto.SessionHistoryPage;
import com.gymtracker.api.exception.ValidationException;
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

    private static final int MAX_PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(SessionHistoryService.class);

    private final LoggedSessionRepository loggedSessionRepository;
    private final DtoMapper dtoMapper;

    public SessionHistoryService(LoggedSessionRepository loggedSessionRepository, DtoMapper dtoMapper) {
        this.loggedSessionRepository = loggedSessionRepository;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Retrieves paginated session history for a user with optional date and exercise filters.
     *
     * @param userId authenticated user identifier
     * @param page zero-based page index
     * @param size page size between 1 and 100
     * @param dateFrom inclusive lower bound for session date filtering
     * @param dateTo inclusive upper bound for session date filtering
     * @param exerciseName optional exercise name fragment filter
     * @return history page with reverse-chronological items
     * @throws ValidationException when paging parameters are out of range
     */
    @Transactional(readOnly = true)
    public SessionHistoryPage getSessionHistory(UUID userId, int page, int size, LocalDate dateFrom, LocalDate dateTo, String exerciseName) {
        if (page < 0) {
            throw new ValidationException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException("Size must be between 1 and 100");
        }
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


