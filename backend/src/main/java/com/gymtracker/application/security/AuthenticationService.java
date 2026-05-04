package com.gymtracker.application.security;

import com.gymtracker.api.exception.UnauthorizedException;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.stereotype.Service;

/**
 * Resolves the currently authenticated user's UUID from Spring Security context.
 */
@Service
public final class AuthenticationService {

    /** Repository used to look up the user by username. */
    private final UserRepository userRepository;

    /**
     * Constructs the service with the given repository.
     *
     * @param repo the user repository
     */
    public AuthenticationService(final UserRepository repo) {
        this.userRepository = repo;
    }

    /**
     * Returns the UUID of the currently authenticated user.
     *
     * @return the authenticated user's UUID
     * @throws UnauthorizedException when no principal exists or user not in DB
     */
    public UUID getCurrentUserId() {
        final Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("No authenticated user present");
        }
        return userRepository.findByUsername(authentication.getName())
            .map(com.gymtracker.domain.User::getId)
            .orElseThrow(() -> new UnauthorizedException(
                "Authenticated user is not mapped to a workout tracker account"
            ));
    }
}
