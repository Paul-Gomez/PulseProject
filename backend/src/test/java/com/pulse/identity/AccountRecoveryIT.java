package com.pulse.identity;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountRecoveryIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class MailConfig {
        @Bean
        @Primary
        CapturingEmailService capturingEmailService() {
            return new CapturingEmailService();
        }
    }

    @Autowired
    private CapturingEmailService mails;

    private String registerAndGetRefreshToken(String email, String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"username\":\"" + username
                                + "\",\"password\":\"supersecret123\",\"displayName\":\"" + username + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    private void postJson(String url, String json, int expectedStatus) throws Exception {
        mockMvc.perform(post(url).contentType("application/json").content(json))
                .andExpect(status().is(expectedStatus));
    }

    private void login(String email, String password, int expectedStatus) throws Exception {
        postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", expectedStatus);
    }

    @Test
    void registering_sendsAVerificationEmail_whoseLinkWorksOnlyOnce() throws Exception {
        String token = registerAndGetAccessToken("verify1@example.com", "verifyuser1");
        String verificationToken = mails.lastTokenSentTo("verify1@example.com").orElseThrow();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.emailVerified").value(false));

        postJson("/api/v1/auth/verify-email", "{\"token\":\"" + verificationToken + "\"}", 204);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.emailVerified").value(true));
        postJson("/api/v1/auth/verify-email", "{\"token\":\"" + verificationToken + "\"}", 400);
    }

    @Test
    void invalidVerificationTokens_areRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType("application/json")
                        .content("{\"token\":\"esto-no-es-un-token-valido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void verificationEmailCanBeRequestedAgain_whileLoggedInAndNotYetVerified() throws Exception {
        String token = registerAndGetAccessToken("verify2@example.com", "verifyuser2");
        assertEquals(1, mails.sentTo("verify2@example.com").size());

        mockMvc.perform(post("/api/v1/users/me/verification-email").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertEquals(2, mails.sentTo("verify2@example.com").size());

        postJson("/api/v1/auth/verify-email",
                "{\"token\":\"" + mails.lastTokenSentTo("verify2@example.com").orElseThrow() + "\"}", 204);
        mockMvc.perform(post("/api/v1/users/me/verification-email").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertEquals(2, mails.sentTo("verify2@example.com").size());

        mockMvc.perform(post("/api/v1/users/me/verification-email")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_looksTheSameForUnknownEmails_andSendsNothingForThem() throws Exception {
        postJson("/api/v1/auth/forgot-password", "{\"email\":\"nadie-registrado@example.com\"}", 204);

        assertTrue(mails.sentTo("nadie-registrado@example.com").isEmpty());
    }

    @Test
    void resettingThePassword_withTheEmailedLink_replacesItAndClosesAllSessions() throws Exception {
        String refreshToken = registerAndGetRefreshToken("reset1@example.com", "resetuser1");

        postJson("/api/v1/auth/forgot-password", "{\"email\":\"reset1@example.com\"}", 204);
        String resetToken = mails.lastTokenSentTo("reset1@example.com").orElseThrow();
        postJson("/api/v1/auth/reset-password", "{\"token\":\"" + resetToken + "\",\"newPassword\":\"claveRecuperada789\"}", 204);

        login("reset1@example.com", "supersecret123", 401);
        login("reset1@example.com", "claveRecuperada789", 200);
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}", 401);
        postJson("/api/v1/auth/reset-password", "{\"token\":\"" + resetToken + "\",\"newPassword\":\"otraMasDistinta000\"}", 400);
    }

    @Test
    void askingForANewResetLink_invalidatesThePreviousOne() throws Exception {
        registerAndGetAccessToken("reset2@example.com", "resetuser2");

        postJson("/api/v1/auth/forgot-password", "{\"email\":\"reset2@example.com\"}", 204);
        String firstToken = mails.lastTokenSentTo("reset2@example.com").orElseThrow();
        postJson("/api/v1/auth/forgot-password", "{\"email\":\"reset2@example.com\"}", 204);
        String secondToken = mails.lastTokenSentTo("reset2@example.com").orElseThrow();

        postJson("/api/v1/auth/reset-password", "{\"token\":\"" + firstToken + "\",\"newPassword\":\"claveRecuperada789\"}", 400);
        postJson("/api/v1/auth/reset-password", "{\"token\":\"" + secondToken + "\",\"newPassword\":\"claveRecuperada789\"}", 204);
    }

    @Test
    void aVerificationLinkCannotBeUsedToResetAPassword() throws Exception {
        registerAndGetAccessToken("reset3@example.com", "resetuser3");
        String verificationToken = mails.lastTokenSentTo("reset3@example.com").orElseThrow();

        postJson("/api/v1/auth/reset-password",
                "{\"token\":\"" + verificationToken + "\",\"newPassword\":\"claveRecuperada789\"}", 400);
    }

    @Test
    void resetPasswordMustBeStrongEnough() throws Exception {
        postJson("/api/v1/auth/reset-password", "{\"token\":\"cualquiera\",\"newPassword\":\"corta\"}", 400);
    }
}
