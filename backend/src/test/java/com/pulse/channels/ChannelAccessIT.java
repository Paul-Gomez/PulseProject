package com.pulse.channels;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelAccessIT extends AbstractIntegrationTest {

    @Test
    void creatorOfPrivateChannel_canAccessIt() throws Exception {
        String ownerToken = registerAndGetAccessToken("access1@example.com", "access1");
        String workspaceId = createWorkspace(ownerToken, "Private Creators");
        String channelId = createChannel(ownerToken, workspaceId, "mi-canal", true);

        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true));
    }

    @Test
    void workspaceMemberAddedToPrivateChannel_canAccessIt() throws Exception {
        String ownerToken = registerAndGetAccessToken("access2@example.com", "access2");
        String memberToken = registerAndGetAccessToken("access2b@example.com", "access2b");
        String workspaceId = createWorkspace(ownerToken, "Invited To Private");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"access2b@example.com\"}"))
                .andExpect(status().isCreated());

        String memberId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + memberToken))
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        String channelId = createChannel(ownerToken, workspaceId, "solo-invitados", true);

        mockMvc.perform(post("/api/v1/channels/" + channelId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/channels/" + channelId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }
}
