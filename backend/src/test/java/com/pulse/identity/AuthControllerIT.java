package com.pulse.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.common.AbstractIntegrationTest;
import com.pulse.identity.dto.LoginRequest;
import com.pulse.identity.dto.RefreshRequest;
import com.pulse.identity.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLoginThenRefresh_returnsValidTokenPairs() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "alice@example.com", "alice", "supersecret123", "Alice");

        String registerBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(registerBody).get("refreshToken").asText();

        LoginRequest loginRequest = new LoginRequest("alice@example.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        // The old refresh token was rotated out and must not be usable a second time.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withDuplicateEmail_returnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest("bob@example.com", "bob", "supersecret123", "Bob");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest("bob@example.com", "bob2", "supersecret123", "Bob2");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        RegisterRequest request = new RegisterRequest("carol@example.com", "carol", "supersecret123", "Carol");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = new LoginRequest("carol@example.com", "wrongpassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMe_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMe_withValidToken_returnsCurrentUser() throws Exception {
        RegisterRequest request = new RegisterRequest("dave@example.com", "dave", "supersecret123", "Dave");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@example.com"));
    }
}
