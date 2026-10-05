package com.pulse.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIT extends AbstractIntegrationTest {

    private void invite(String ownerToken, String workspaceId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
    }

    private void sendMessage(String token, String channelId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated());
    }

    private JsonNode notifications(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content");
    }

    private long unreadCount(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("count").asLong();
    }

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void mentioningAUser_createsAMentionNotificationForThem() throws Exception {
        String ownerToken = registerAndGetAccessToken("noti1@example.com", "notiowner1");
        String memberToken = registerAndGetAccessToken("noti1b@example.com", "notimember1");
        String workspaceId = createWorkspace(ownerToken, "Mentions Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        invite(ownerToken, workspaceId, "noti1b@example.com");

        sendMessage(ownerToken, channelId, "hola @notimember1, mira esto");

        // Newest first: the mention, then the invitation that added them to the workspace.
        JsonNode list = notifications(memberToken);
        org.junit.jupiter.api.Assertions.assertEquals(2, list.size());
        org.junit.jupiter.api.Assertions.assertEquals("MENTION", list.get(0).get("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals(channelId, list.get(0).get("channelId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(2, unreadCount(memberToken));
    }

    @Test
    void mentioningYourselfOrSomeoneWhoCannotSeeThePrivateChannel_createsNothing() throws Exception {
        String ownerToken = registerAndGetAccessToken("noti2@example.com", "notiowner2");
        String memberToken = registerAndGetAccessToken("noti2b@example.com", "notimember2");
        String workspaceId = createWorkspace(ownerToken, "Private Mentions");
        String privateChannelId = createChannel(ownerToken, workspaceId, "secreto", true);
        invite(ownerToken, workspaceId, "noti2b@example.com");

        sendMessage(ownerToken, privateChannelId, "@notimember2 esto no deberias verlo");
        sendMessage(ownerToken, privateChannelId, "me menciono a mi mismo @notiowner2");

        // The only notification the member has is the invitation; nobody got a mention.
        JsonNode memberNotifications = notifications(memberToken);
        org.junit.jupiter.api.Assertions.assertEquals(1, memberNotifications.size());
        org.junit.jupiter.api.Assertions.assertEquals("INVITATION", memberNotifications.get(0).get("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals(0, unreadCount(ownerToken));
    }

    @Test
    void beingInvitedRemovedOrModerated_createsInvitationAndModerationNotifications() throws Exception {
        String ownerToken = registerAndGetAccessToken("noti3@example.com", "notiowner3");
        String memberToken = registerAndGetAccessToken("noti3b@example.com", "notimember3");
        String workspaceId = createWorkspace(ownerToken, "Moderation Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");

        invite(ownerToken, workspaceId, "noti3b@example.com");
        org.junit.jupiter.api.Assertions.assertEquals("INVITATION", notifications(memberToken).get(0).get("type").asText());

        String messageBody = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json")
                        .content("{\"content\":\"mensaje inapropiado\"}"))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(messageBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/messages/" + messageId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertEquals("MODERATION", notifications(memberToken).get(0).get("type").asText());

        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId + "/members/" + userId(memberToken))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertEquals(3, unreadCount(memberToken));
    }

    @Test
    void markingNotificationsAsRead_updatesTheCounter_andOnlyTheOwnerCanDoIt() throws Exception {
        String ownerToken = registerAndGetAccessToken("noti4@example.com", "notiowner4");
        String memberToken = registerAndGetAccessToken("noti4b@example.com", "notimember4");
        String workspaceId = createWorkspace(ownerToken, "Read Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        invite(ownerToken, workspaceId, "noti4b@example.com");
        sendMessage(ownerToken, channelId, "primero @notimember4");
        sendMessage(ownerToken, channelId, "segundo @notimember4");
        org.junit.jupiter.api.Assertions.assertEquals(3, unreadCount(memberToken));

        String notificationId = notifications(memberToken).get(0).get("id").asText();

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(2, unreadCount(memberToken));

        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertEquals(0, unreadCount(memberToken));

        mockMvc.perform(get("/api/v1/notifications?unreadOnly=true").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }
}
