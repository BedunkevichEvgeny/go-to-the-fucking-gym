package com.gymtracker.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void nextProgramSessionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/program-sessions/next"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nextProgramSessionReturnsSeededSessionForUser1() throws Exception {
        mockMvc.perform(get("/api/program-sessions/next").with(httpBasic("user1", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Upper Body"))
                .andExpect(jsonPath("$.exercises.length()").value(3));
    }
}

