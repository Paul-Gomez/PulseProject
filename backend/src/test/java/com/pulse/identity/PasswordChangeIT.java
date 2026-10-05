package com.pulse.identity;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordChangeIT extends AbstractIntegrationTest {

    private String registerAndGetRefreshToken(String email, String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"username\":\"" + username
                                + "\",\"password\":\"supersecret123\",\"displayName\":\"" + username + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    private void changePassword(String token, String current, String next, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void login(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void changingThePassword_replacesIt_andSignsOutEverySession() throws Exception {
        String refreshToken = registerAndGetRefreshToken("pwd1@example.com", "pwduser1");
        registerAndGetAccessToken("pwd1b@example.com", "pwduser1b");

        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"pwd1@example.com\",\"password\":\"supersecret123\"}"))
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();

        changePassword(token, "supersecret123", "otraClaveNueva456", 204);

        login("pwd1@example.com", "supersecret123", 401);
        login("pwd1@example.com", "otraClaveNueva456", 200);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        // Someone else's account is untouched.
        login("pwd1b@example.com", "supersecret123", 200);
    }

    @Test
    void wrongCurrentPassword_isRejected() throws Exception {
        String token = registerAndGetAccessToken("pwd2@example.com", "pwduser2");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"no-es-esta\",\"newPassword\":\"otraClaveNueva456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
        login("pwd2@example.com", "supersecret123", 200);
    }

    @Test
    void newPasswordMustBeStrongEnoughAndDifferent() throws Exception {
        String token = registerAndGetAccessToken("pwd3@example.com", "pwduser3");

        changePassword(token, "supersecret123", "corta", 400);
        changePassword(token, "supersecret123", "supersecret123", 400);
    }

    @Test
    void changingThePasswordRequiresLogin() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"a\",\"newPassword\":\"b\"}"))
                .andExpect(status().isUnauthorized());
    }
}
