package com.gymtracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.User;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for POST /api/auth/login, GET /api/auth/me, POST /api/auth/logout.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @LocalServerPort
    private int port;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private static final User ADMIN = User.builder()
        .id(ADMIN_ID)
        .username("admin")
        .password("admin")
        .preferredWeightUnit(WeightUnit.KG)
        .build();

    /** Shared HttpClient that preserves cookies across requests. */
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        // Cookie-aware client for session lifecycle tests
        httpClient = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(ADMIN));
        Mockito.when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(ADMIN));
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private HttpResponse<String> postJson(final String path, final String body) throws Exception {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(final String path) throws Exception {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(base() + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    // ── User Story 2: Error feedback ─────────────────────────────────────────

    @Test
    void login_badCredentials_returns401WithGenericError() throws Exception {
        final HttpResponse<String> response =
            postJson("/api/auth/login", "{\"username\":\"admin\",\"password\":\"wrong\"}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("Invalid credentials");
    }

    @Test
    void login_unknownUser_returns401GenericError_doesNotRevealField() throws Exception {
        Mockito.when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        final HttpResponse<String> response =
            postJson("/api/auth/login", "{\"username\":\"nobody\",\"password\":\"admin\"}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("Invalid credentials");
    }

    // ── User Story 3: Session lifecycle ──────────────────────────────────────

    @Test
    void getMe_withoutSession_returns401() throws Exception {
        final HttpResponse<String> response = get("/api/auth/me");
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void loginThenGetMe_returns200WithUsername() throws Exception {
        final HttpResponse<String> loginResp =
            postJson("/api/auth/login", "{\"username\":\"admin\",\"password\":\"admin\"}");
        assertThat(loginResp.statusCode()).isEqualTo(200);

        final HttpResponse<String> meResp = get("/api/auth/me");
        assertThat(meResp.statusCode()).isEqualTo(200);
        assertThat(meResp.body()).contains("admin");
    }

    @Test
    void loginThenLogoutThenGetMe_returns401() throws Exception {
        postJson("/api/auth/login", "{\"username\":\"admin\",\"password\":\"admin\"}");

        final HttpResponse<String> logoutResp =
            postJson("/api/auth/logout", "");
        assertThat(logoutResp.statusCode()).isEqualTo(200);

        final HttpResponse<String> meResp = get("/api/auth/me");
        assertThat(meResp.statusCode()).isEqualTo(401);
    }
}
