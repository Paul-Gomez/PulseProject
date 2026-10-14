package com.pulse.channels;

import com.pulse.channels.dto.CreateChannelRequest;
import com.pulse.channels.entity.ChannelType;
import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnouncementChannelIT extends AbstractIntegrationTest {

    @Test
    void onlyModeratorsAndAboveCanPostInAnnouncementChannels() throws Exception {
        String ownerToken = registerAndGetAccessToken("announce1@example.com", "announce1");
        String memberToken = registerAndGetAccessToken("announce1b@example.com", "announce1b");
        String workspaceId = createWorkspace(ownerToken, "Announcements Team");
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"announce1b@example.com\"}"))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/channels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateChannelRequest("anuncios", ChannelType.ANNOUNCEMENT, false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String channelId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json")
                        .content("{\"content\":\"puedo escribir aqui?\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"content\":\"anuncio oficial\"}"))
                .andExpect(status().isCreated());
    }
}
