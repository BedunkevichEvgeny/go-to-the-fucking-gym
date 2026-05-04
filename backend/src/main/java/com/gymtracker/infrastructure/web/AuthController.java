package com.gymtracker.infrastructure.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication state queries.
 *
 * <p>{@code GET /api/auth/me} is a protected endpoint.
 * Returns HTTP 401 when unauthenticated —
 * used by the frontend as the auth-check signal.
 */
@RestController
public class AuthController {

    /**
     * Returns the currently authenticated user's username.
     *
     * @return {@link AuthMeResponse} with the username
     */
    @GetMapping("/api/auth/me")
    public AuthMeResponse me() {
        final Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        return new AuthMeResponse(auth.getName());
    }

    /**
     * Response payload for {@code GET /api/auth/me}.
     *
     * @param username the authenticated user's username
     */
    public record AuthMeResponse(String username) {
    }
}
