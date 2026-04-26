package com.gymtracker.application.security;

import com.gymtracker.api.exception.UnauthorizedException;
import com.gymtracker.infrastructure.config.SecurityUsersProperties;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final SecurityUsersProperties securityUsersProperties;

    public AuthenticationService(SecurityUsersProperties securityUsersProperties) {
        this.securityUsersProperties = securityUsersProperties;
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("No authenticated user present");
        }
        SecurityUsersProperties.UserDefinition userDefinition = securityUsersProperties.getUsers().get(authentication.getName());
        if (userDefinition == null || userDefinition.getId() == null) {
            throw new UnauthorizedException("Authenticated user is not mapped to a workout tracker account");
        }
        return userDefinition.getId();
    }
}

