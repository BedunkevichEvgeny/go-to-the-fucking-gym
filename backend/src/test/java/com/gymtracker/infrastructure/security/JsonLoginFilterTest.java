package com.gymtracker.infrastructure.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.gymtracker.domain.User;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for JsonLoginFilter: verifies login endpoint with valid and invalid credentials.
 */
@SpringBootTest
class JsonLoginFilterTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder()
        .id(UUID.randomUUID())
        .username("admin")
        .password("admin")
        .preferredWeightUnit(WeightUnit.KG)
        .build();

    @Test
    void login_validCredentials_returns200EmptyJson() throws Exception {
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(ADMIN));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void login_badCredentials_returns401WithErrorMessage() throws Exception {
        Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(ADMIN));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_unknownUser_returns401WithErrorMessage() throws Exception {
        Mockito.when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody\",\"password\":\"any\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }
}
