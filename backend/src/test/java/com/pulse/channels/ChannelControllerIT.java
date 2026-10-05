package com.pulse.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.channels.dto.CreateChannelRequest;
import com.pulse.channels.entity.ChannelType;
import com.pulse.common.AbstractIntegrationTest;
import com.pulse.identity.dto.RegisterRequest;
import com.pulse.workspaces.dto.CreateWorkspaceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelControllerIT extends AbstractIntegrationTest {

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

    private String createWorkspace(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceRequest(name, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void createPublicChannel_isVisibleToAnyWorkspaceMember() throws Exception {
        String ownerToken = registerAndGetAccessToken("chowner1@example.com", "chowner1");
        String workspaceId = createWorkspace(ownerToken, "Dev Team");

        String body = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/channels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("general", ChannelType.TEXT, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("general"))
                .andReturn().getResponse().getContentAsString();

        JsonNode channel = objectMapper.readTree(body);

        mockMvc.perform(get("/api/v1/channels/" + channel.get("id").asText())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void createChannel_withDuplicateName_returnsConflict() throws Exception {
        String ownerToken = registerAndGetAccessToken("chowner2@example.com", "chowner2");
        String workspaceId = createWorkspace(ownerToken, "Duplicate Names Inc");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/channels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("general", ChannelType.TEXT, false))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/channels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("general", ChannelType.TEXT, false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHANNEL_NAME_TAKEN"));
    }

    @Test
    void privateChannel_isNotAccessibleToWorkspaceMemberWhoWasNotAdded() throws Exception {
        String ownerToken = registerAndGetAccessToken("chowner3@example.com", "chowner3");
        String memberToken = registerAndGetAccessToken("chmember3@example.com", "chmember3");
        String workspaceId = createWorkspace(ownerToken, "Secretive Org");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"chmember3@example.com\"}"))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/channels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("secret-plans", ChannelType.TEXT, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String channelId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
