package com.gymtracker.application;

import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionDetailService {

    private final LoggedSessionRepository loggedSessionRepository;
    private final DtoMapper dtoMapper;

    public SessionDetailService(LoggedSessionRepository loggedSessionRepository, DtoMapper dtoMapper) {
        this.loggedSessionRepository = loggedSessionRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public LoggedSessionDetail getSessionDetails(UUID userId, UUID sessionId) {
        var session = loggedSessionRepository.findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Logged session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("Logged session does not belong to the authenticated user");
        }
        return dtoMapper.toDetailDto(session);
    }
}

