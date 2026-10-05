package com.pulse.common;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UnauthenticatedAccessIT extends AbstractIntegrationTest {

    @Test
    void requestWithoutToken_returns401WithOurErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/workspaces"));
    }

    @Test
    void requestWithGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces").header("Authorization", "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithValidToken_isAllowed() throws Exception {
        String token = registerAndGetAccessToken("unauth1@example.com", "unauth1");

        mockMvc.perform(get("/api/v1/workspaces").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
