package com.gymtracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Login filter that reads credentials from a JSON request body.
 *
 * <p>Endpoint: {@code POST /api/auth/login}
 * <p>Success: HTTP 200 with empty JSON body {@code {}}
 * <p>Failure: HTTP 401 with {@code {"error":"Invalid credentials"}}
 */
public final class JsonLoginFilter
    extends UsernamePasswordAuthenticationFilter {

    /** Jackson mapper used to read the JSON request body and write errors. */
    private final ObjectMapper objectMapper;

    /**
     * Constructs the filter.
     *
     * @param authManager  the authentication manager
     * @param mapper       Jackson ObjectMapper for JSON parsing and writing
     */
    public JsonLoginFilter(
        final AuthenticationManager authManager,
        final ObjectMapper mapper) {
        super(authManager);
        setFilterProcessesUrl("/api/auth/login");
        this.objectMapper = mapper;
        configureHandlers();
    }

    /**
     * Configures success and failure handlers for JSON responses.
     */
    private void configureHandlers() {
        setAuthenticationSuccessHandler((req, res, auth) -> {
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json");
            res.getWriter().write("{}");
        });

        setAuthenticationFailureHandler((req, res, ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter()
                .write(objectMapper.writeValueAsString(
                    Map.of("error", "Invalid credentials")));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authentication attemptAuthentication(
        final HttpServletRequest request,
        final HttpServletResponse response) throws AuthenticationException {
        try {
            @SuppressWarnings("unchecked")
            final Map<String, String> body =
                objectMapper.readValue(request.getInputStream(), Map.class);
            final String username = body.getOrDefault("username", "");
            final String password = body.getOrDefault("password", "");
            final UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(username, password);
            return getAuthenticationManager().authenticate(token);
        } catch (IOException ex) {
            throw new org.springframework.security.authentication
                .AuthenticationServiceException(
                    "Failed to parse authentication request body", ex);
        }
    }
}
