package com.pulse.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHardeningIT extends AbstractIntegrationTest {

    private record Team(String ownerToken, String memberToken, String memberId, String workspaceId) {
    }

    private Team team(String prefix) throws Exception {
        String ownerToken = registerAndGetAccessToken(prefix + "@example.com", prefix + "owner");
        String memberToken = registerAndGetAccessToken(prefix + "b@example.com", prefix + "member");
        String workspaceId = createWorkspace(ownerToken, "Team " + prefix);
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + prefix + "b@example.com\"}"))
                .andExpect(status().isCreated());
        String memberId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        return new Team(ownerToken, memberToken, memberId, workspaceId);
    }

    @Test
    void aMemberCannotAddThemselvesToAPrivateChannel() throws Exception {
        Team team = team("harden1");
        String privateChannelId = createChannel(team.ownerToken(), team.workspaceId(), "secreto", true);

        mockMvc.perform(post("/api/v1/channels/" + privateChannelId + "/members/" + team.memberId())
                        .header("Authorization", "Bearer " + team.memberToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/channels/" + privateChannelId)
                        .header("Authorization", "Bearer " + team.memberToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMemberCannotRemoveOthersFromAChannel_butCanLeaveIt() throws Exception {
        Team team = team("harden2");
        String channelId = createChannel(team.ownerToken(), team.workspaceId(), "equipo", true);
        String ownerId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + team.ownerToken()))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/channels/" + channelId + "/members/" + team.memberId())
                        .header("Authorization", "Bearer " + team.ownerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/channels/" + channelId + "/members/" + ownerId)
                        .header("Authorization", "Bearer " + team.memberToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/channels/" + channelId + "/members/" + team.memberId())
                        .header("Authorization", "Bearer " + team.memberToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void channelListDoesNotRevealPrivateChannelsYouAreNotIn() throws Exception {
        Team team = team("harden3");
        createPublicChannel(team.ownerToken(), team.workspaceId(), "general");
        createChannel(team.ownerToken(), team.workspaceId(), "solo-direccion", true);

        String body = mockMvc.perform(get("/api/v1/workspaces/" + team.workspaceId() + "/channels")
                        .header("Authorization", "Bearer " + team.memberToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode channels = objectMapper.readTree(body);

        assertEquals(1, channels.size());
        assertEquals("general", channels.get(0).get("name").asText());
    }

    @Test
    void otherUsersProfilesDoNotIncludeTheirEmail() throws Exception {
        Team team = team("harden4");

        String body = mockMvc.perform(get("/api/v1/users/" + team.memberId())
                        .header("Authorization", "Bearer " + team.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("harden4member"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("harden4b@example.com"));
    }

    @Test
    void archivedChannelsDoNotAcceptNewMessages() throws Exception {
        Team team = team("harden5");
        String channelId = createPublicChannel(team.ownerToken(), team.workspaceId(), "viejo");
        mockMvc.perform(delete("/api/v1/channels/" + channelId).header("Authorization", "Bearer " + team.ownerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + team.ownerToken())
                        .contentType("application/json")
                        .content("{\"content\":\"hola?\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHANNEL_ARCHIVED"));
    }

    @Test
    void invalidSortReturns400_andPageSizeIsCapped() throws Exception {
        Team team = team("harden6");

        mockMvc.perform(get("/api/v1/workspaces/" + team.workspaceId() + "/members?sort=passwordHash")
                        .header("Authorization", "Bearer " + team.ownerToken()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/workspaces/" + team.workspaceId() + "/members?size=5000")
                        .header("Authorization", "Bearer " + team.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
