package com.gymtracker.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.infrastructure.security.AppUserDetailsService;
import com.gymtracker.infrastructure.security.JsonLoginFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for the Workout Tracker REST API.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>CSRF disabled — REST JSON API; no browser form submissions.</li>
 *   <li>NoOpPasswordEncoder — plain text comparison
 *       (MVP only, NOT for production).</li>
 *   <li>Server-side HttpSession with JSESSIONID cookie — no JWT.</li>
 *   <li>Only POST /api/auth/login and POST /api/auth/logout are public.
 *       GET /api/auth/me returns HTTP 401 when unauthenticated.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
    SecurityUsersProperties.class,
    AzureOpenAiOnboardingProperties.class
})
public class SecurityConfig {

    /** User details service backed by the database. */
    private final AppUserDetailsService userDetailsService;

    /** Jackson mapper used to write JSON error responses. */
    private final ObjectMapper objectMapper;

    /**
     * Constructs the configuration with required dependencies.
     *
     * @param svc    user details service backed by the {@code users} table
     * @param mapper Jackson ObjectMapper for writing JSON error responses
     */
    public SecurityConfig(
        final AppUserDetailsService svc,
        final ObjectMapper mapper) {
        this.userDetailsService = svc;
        this.objectMapper = mapper;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean.
     *
     * @param config Spring's authentication configuration
     * @return the configured {@link AuthenticationManager}
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
        final AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures the Spring Security filter chain.
     *
     * @param http                  the {@link HttpSecurity} builder
     * @param authManager           the authentication manager
     * @param corsConfigSource CORS configuration source     * @return the built {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        final HttpSecurity http,
        final AuthenticationManager authManager,
        @Qualifier("corsConfigurationSource") final CorsConfigurationSource corsConfigSource) throws Exception {

        final JsonLoginFilter loginFilter =
            new JsonLoginFilter(authManager, objectMapper);
        // Explicitly save authentication to HttpSession on successful login (Spring Security 6+)
        loginFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        http
            // CSRF disabled — REST JSON API, no browser form submissions
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigSource))
            .securityContext(ctx -> ctx
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                    .permitAll()
                .anyRequest().authenticated()
            )
            .addFilterAt(
                loginFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write("{}");
                })
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    // Return JSON 401 so the frontend can detect unauthenticated
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter()
                        .write(objectMapper.writeValueAsString(
                            Map.of("error", "Unauthorized")));
                })
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * CORS configuration allowing the Vite dev server to call the API.
     *
     * @param allowedOrigins comma-separated list of allowed origins
     * @return the {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${cors.allowed-origins:http://localhost:5173}")
        final String allowedOrigins) {
        final CorsConfiguration config = new CorsConfiguration();
        for (final String origin : allowedOrigins.split(",")) {
            config.addAllowedOrigin(origin.trim());
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        final UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Password encoder bean using NoOpPasswordEncoder (MVP only — plain text).
     *
     * @return the {@link PasswordEncoder}
     */
    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        // NoOpPasswordEncoder is intentional for this MVP.
        // DO NOT use in production.
        return NoOpPasswordEncoder.getInstance();
    }
}
