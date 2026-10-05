package com.pulse.channels;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelAccessAfterRemovalIT extends AbstractIntegrationTest {

    private void invite(String ownerToken, String workspaceId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
    }

    private void channelAccess(String token, String channelId, int expectedStatus) throws Exception {
        mockMvc.perform(get("/api/v1/channels/" + channelId).header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void removedMember_losesPrivateChannelAccess_andDoesNotGetItBackWhenReinvited() throws Exception {
        String ownerToken = registerAndGetAccessToken("removal1@example.com", "removal1");
        String memberToken = registerAndGetAccessToken("removal1b@example.com", "removal1b");
        String workspaceId = createWorkspace(ownerToken, "Removal Team");
        String publicChannelId = createPublicChannel(ownerToken, workspaceId, "general");
        String privateChannelId = createChannel(ownerToken, workspaceId, "secreto", true);

        invite(ownerToken, workspaceId, "removal1b@example.com");
        String memberId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + memberToken))
                        .andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/channels/" + privateChannelId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        channelAccess(memberToken, privateChannelId, 200);

        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        channelAccess(memberToken, privateChannelId, 403);
        channelAccess(memberToken, publicChannelId, 403);

        invite(ownerToken, workspaceId, "removal1b@example.com");

        channelAccess(memberToken, publicChannelId, 200);
        channelAccess(memberToken, privateChannelId, 403);
    }
}
