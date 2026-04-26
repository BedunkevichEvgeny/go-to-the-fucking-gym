package com.gymtracker.application;

import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.SessionType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FreeSessionService {

    private final LoggedSessionService loggedSessionService;

    public FreeSessionService(LoggedSessionService loggedSessionService) {
        this.loggedSessionService = loggedSessionService;
    }

    public LoggedSessionDetail saveFreeSession(UUID userId, LoggedSessionCreateRequest request) {
        if (request.sessionType() != SessionType.FREE) {
            throw new ValidationException("FreeSessionService only accepts FREE sessions");
        }
        return loggedSessionService.saveLoggedSession(userId, request);
    }
}

