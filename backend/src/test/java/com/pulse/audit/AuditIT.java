package com.pulse.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditIT extends AbstractIntegrationTest {

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                      String token, String body, int expectedStatus) throws Exception {
        var builder = request.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder = builder.contentType("application/json").content(body);
        }
        mockMvc.perform(builder).andExpect(status().is(expectedStatus));
    }

    private JsonNode auditLog(String token, String workspaceId, String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/audit-logs" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void administrativeActionsAreRecordedWithWhoDidWhat() throws Exception {
        String ownerToken = registerAndGetAccessToken("audit1@example.com", "auditowner1");
        String memberToken = registerAndGetAccessToken("audit1b@example.com", "auditmember1");
        String workspaceId = createWorkspace(ownerToken, "Audit Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "temporal");
        String memberId = userId(memberToken);

        call(post("/api/v1/workspaces/" + workspaceId + "/members"), ownerToken, "{\"email\":\"audit1b@example.com\"}", 201);
        call(patch("/api/v1/workspaces/" + workspaceId + "/members/" + memberId + "/role"), ownerToken,
                "{\"roleName\":\"MODERATOR\"}", 200);
        call(patch("/api/v1/workspaces/" + workspaceId), ownerToken, "{\"name\":\"Audit Team Renamed\"}", 200);
        call(delete("/api/v1/channels/" + channelId), ownerToken, null, 204);
        call(post("/api/v1/workspaces/" + workspaceId + "/bans/" + memberId), ownerToken, "{\"reason\":\"spam\"}", 201);
        call(delete("/api/v1/workspaces/" + workspaceId + "/bans/" + memberId), ownerToken, null, 204);

        JsonNode logs = auditLog(ownerToken, workspaceId, "");

        Set<String> actions = new HashSet<>();
        logs.get("content").forEach(entry -> actions.add(entry.get("action").asText()));
        assertEquals(Set.of("MEMBER_INVITED", "MEMBER_ROLE_CHANGED", "WORKSPACE_UPDATED", "CHANNEL_ARCHIVED",
                "MEMBER_BANNED", "MEMBER_UNBANNED"), actions);

        // Newest first, and each entry says who did it.
        assertEquals("MEMBER_UNBANNED", logs.get("content").get(0).get("action").asText());
        assertEquals("auditowner1", logs.get("content").get(0).get("actorUsername").asText());
        assertEquals(memberId, logs.get("content").get(0).get("resourceId").asText());
    }

    @Test
    void roleChangeAndBanEntriesKeepTheirDetails_butMessageContentIsNeverStored() throws Exception {
        String ownerToken = registerAndGetAccessToken("audit2@example.com", "auditowner2");
        String memberToken = registerAndGetAccessToken("audit2b@example.com", "auditmember2");
        String workspaceId = createWorkspace(ownerToken, "Details Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        call(post("/api/v1/workspaces/" + workspaceId + "/members"), ownerToken, "{\"email\":\"audit2b@example.com\"}", 201);
        call(patch("/api/v1/workspaces/" + workspaceId + "/members/" + userId(memberToken) + "/role"), ownerToken,
                "{\"roleName\":\"ADMIN\"}", 200);

        String messageBody = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json")
                        .content("{\"content\":\"contenido privado muy delicado\"}"))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(messageBody).get("id").asText();
        call(delete("/api/v1/messages/" + messageId), ownerToken, null, 204);

        JsonNode roleChanges = auditLog(ownerToken, workspaceId, "?action=MEMBER_ROLE_CHANGED");
        assertEquals(1, roleChanges.get("totalElements").asInt());
        assertEquals("MEMBER -> ADMIN", roleChanges.get("content").get(0).get("details").asText());

        JsonNode moderation = auditLog(ownerToken, workspaceId, "?action=MESSAGE_DELETED_BY_MODERATOR");
        assertEquals(1, moderation.get("totalElements").asInt());
        assertFalse(moderation.toString().contains("contenido privado muy delicado"));
        assertEquals(messageId, moderation.get("content").get(0).get("resourceId").asText());
    }

    @Test
    void onlyOwnersAndAdminsCanReadTheAuditLog() throws Exception {
        String ownerToken = registerAndGetAccessToken("audit3@example.com", "auditowner3");
        String adminToken = registerAndGetAccessToken("audit3b@example.com", "auditadmin3");
        String moderatorToken = registerAndGetAccessToken("audit3c@example.com", "auditmod3");
        String outsiderToken = registerAndGetAccessToken("audit3d@example.com", "auditoutsider3");
        String workspaceId = createWorkspace(ownerToken, "Restricted Team");
        call(post("/api/v1/workspaces/" + workspaceId + "/members"), ownerToken, "{\"email\":\"audit3b@example.com\"}", 201);
        call(post("/api/v1/workspaces/" + workspaceId + "/members"), ownerToken, "{\"email\":\"audit3c@example.com\"}", 201);
        call(patch("/api/v1/workspaces/" + workspaceId + "/members/" + userId(adminToken) + "/role"), ownerToken,
                "{\"roleName\":\"ADMIN\"}", 200);
        call(patch("/api/v1/workspaces/" + workspaceId + "/members/" + userId(moderatorToken) + "/role"), ownerToken,
                "{\"roleName\":\"MODERATOR\"}", 200);

        assertTrue(auditLog(ownerToken, workspaceId, "").get("totalElements").asInt() > 0);
        assertTrue(auditLog(adminToken, workspaceId, "").get("totalElements").asInt() > 0);
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/audit-logs")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/audit-logs")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogIsPaginated() throws Exception {
        String ownerToken = registerAndGetAccessToken("audit4@example.com", "auditowner4");
        String workspaceId = createWorkspace(ownerToken, "Paged Audit");
        for (int i = 1; i <= 3; i++) {
            call(patch("/api/v1/workspaces/" + workspaceId), ownerToken, "{\"name\":\"Nombre " + i + "\"}", 200);
        }

        JsonNode firstPage = auditLog(ownerToken, workspaceId, "?size=2&page=0");

        assertEquals(3, firstPage.get("totalElements").asInt());
        assertEquals(2, firstPage.get("totalPages").asInt());
        assertEquals(2, firstPage.get("content").size());
    }
}
