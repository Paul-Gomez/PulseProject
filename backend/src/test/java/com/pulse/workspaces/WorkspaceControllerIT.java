package com.pulse.workspaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.common.AbstractIntegrationTest;
import com.pulse.identity.dto.RegisterRequest;
import com.pulse.workspaces.dto.CreateWorkspaceRequest;
import com.pulse.workspaces.dto.UpdateWorkspaceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkspaceControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetAccessToken(String email, String username) throws Exception {
        RegisterRequest request = new RegisterRequest(email, username, "supersecret123", username);
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void createWorkspace_makesCreatorTheOwner() throws Exception {
        String token = registerAndGetAccessToken("owner1@example.com", "owner1");

        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceRequest("Gaming Club", "For gamers"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaming Club"))
                .andReturn().getResponse().getContentAsString();

        JsonNode workspace = objectMapper.readTree(body);
        String workspaceId = workspace.get("id").asText();

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roleName").value("OWNER"));
    }

    @Test
    void updateWorkspace_asNonMember_isForbidden() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner2@example.com", "owner2");
        String outsiderToken = registerAndGetAccessToken("outsider1@example.com", "outsider1");

        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceRequest("Private HQ", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String workspaceId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateWorkspaceRequest("Hijacked", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listWorkspaces_onlyReturnsWorkspacesUserBelongsTo() throws Exception {
        String token = registerAndGetAccessToken("owner3@example.com", "owner3");

        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceRequest("Solo Space", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Solo Space"));
    }
}
