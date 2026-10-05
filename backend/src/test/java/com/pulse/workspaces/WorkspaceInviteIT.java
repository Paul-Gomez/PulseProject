package com.pulse.workspaces;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceInviteIT extends AbstractIntegrationTest {

    @Test
    void inviteSameUserTwice_returnsConflict() throws Exception {
        String ownerToken = registerAndGetAccessToken("invite1@example.com", "invite1");
        registerAndGetAccessToken("invite1b@example.com", "invite1b");
        String workspaceId = createWorkspace(ownerToken, "Doble Invitacion");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"invite1b@example.com\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"invite1b@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_ALREADY_EXISTS"));
    }

    @Test
    void inviteUnknownEmail_returnsNotFound() throws Exception {
        String ownerToken = registerAndGetAccessToken("invite2@example.com", "invite2");
        String workspaceId = createWorkspace(ownerToken, "Email Inexistente");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"nadie@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
