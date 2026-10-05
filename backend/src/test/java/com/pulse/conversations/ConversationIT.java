package com.pulse.conversations;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationIT extends AbstractIntegrationTest {

    private record Person(String token, String id) {
    }

    /** Registers a user and puts them in the given workspace (creating it for the first caller). */
    private Person person(String name, String ownerToken, String workspaceId) throws Exception {
        String token = registerAndGetAccessToken(name + "@example.com", name);
        String id = userId(token);
        if (workspaceId != null) {
            mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType("application/json")
                            .content("{\"email\":\"" + name + "@example.com\"}"))
                    .andExpect(status().isCreated());
        }
        return new Person(token, id);
    }

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private JsonNode start(String token, List<String> participantIds, int expectedStatus) throws Exception {
        String ids = String.join("\",\"", participantIds);
        String body = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"participantIds\":[\"" + ids + "\"]}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode send(String token, String conversationId, String content, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private long unread(String token, String conversationId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode conversation : objectMapper.readTree(body).get("content")) {
            if (conversation.get("id").asText().equals(conversationId)) {
                return conversation.get("unreadCount").asLong();
            }
        }
        throw new AssertionError("conversation not listed");
    }

    @Test
    void startingTheSameChatTwice_fromEitherSide_returnsTheSameConversation() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm1@example.com", "dmowner1");
        String workspaceId = createWorkspace(ownerToken, "DM Team");
        Person bob = person("dmbob1", ownerToken, workspaceId);
        String ownerId = userId(ownerToken);

        JsonNode first = start(ownerToken, List.of(bob.id()), 200);
        JsonNode again = start(ownerToken, List.of(bob.id()), 200);
        JsonNode fromBob = start(bob.token(), List.of(ownerId), 200);

        assertEquals(first.get("id").asText(), again.get("id").asText());
        assertEquals(first.get("id").asText(), fromBob.get("id").asText());
        assertEquals(false, first.get("isGroup").asBoolean());
        assertEquals(2, first.get("memberIds").size());
    }

    @Test
    void twoPeopleOpeningTheChatAtTheSameTime_endUpInOneConversation() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm2@example.com", "dmowner2");
        String workspaceId = createWorkspace(ownerToken, "Race Team");
        Person bob = person("dmbob2", ownerToken, workspaceId);
        String ownerId = userId(ownerToken);

        CompletableFuture<JsonNode> a = CompletableFuture.supplyAsync(() -> {
            try {
                return start(ownerToken, List.of(bob.id()), 200);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<JsonNode> b = CompletableFuture.supplyAsync(() -> {
            try {
                return start(bob.token(), List.of(ownerId), 200);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(a.get().get("id").asText(), b.get().get("id").asText());
    }

    @Test
    void cannotMessageSomeoneYouShareNoWorkspaceWith_norYourself() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm3@example.com", "dmowner3");
        Person stranger = person("dmstranger3", null, null);

        start(ownerToken, List.of(stranger.id()), 403);
        start(ownerToken, List.of(userId(ownerToken)), 400);
    }

    @Test
    void messagesAreListedNewestFirst_andUnreadCountsFollowTheReadMark() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm4@example.com", "dmowner4");
        String workspaceId = createWorkspace(ownerToken, "Unread Team");
        Person bob = person("dmbob4", ownerToken, workspaceId);
        String conversationId = start(ownerToken, List.of(bob.id()), 200).get("id").asText();

        send(ownerToken, conversationId, "primero", 201);
        send(ownerToken, conversationId, "segundo", 201);

        assertEquals(2, unread(bob.token(), conversationId));
        assertEquals(0, unread(ownerToken, conversationId));

        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("segundo"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(patch("/api/v1/conversations/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isNoContent());
        assertEquals(0, unread(bob.token(), conversationId));

        send(ownerToken, conversationId, "tercero", 201);
        assertEquals(1, unread(bob.token(), conversationId));
    }

    @Test
    void outsidersCannotReadOrWriteInAConversation() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm5@example.com", "dmowner5");
        String workspaceId = createWorkspace(ownerToken, "Privacy Team");
        Person bob = person("dmbob5", ownerToken, workspaceId);
        Person eve = person("dmeve5", ownerToken, workspaceId);
        String conversationId = start(ownerToken, List.of(bob.id()), 200).get("id").asText();
        send(ownerToken, conversationId, "secreto", 201);

        send(eve.token(), conversationId, "me cuelo", 404);
        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + eve.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAuthorCanDeleteAMessage_andDeletedMessagesDisappear() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm6@example.com", "dmowner6");
        String workspaceId = createWorkspace(ownerToken, "Delete Team");
        Person bob = person("dmbob6", ownerToken, workspaceId);
        String conversationId = start(ownerToken, List.of(bob.id()), 200).get("id").asText();
        String messageId = send(ownerToken, conversationId, "borrame", 201).get("id").asText();

        mockMvc.perform(delete("/api/v1/direct-messages/" + messageId).header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/direct-messages/" + messageId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + bob.token()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void repliesMustPointToAMessageOfTheSameConversation() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm7@example.com", "dmowner7");
        String workspaceId = createWorkspace(ownerToken, "Reply Team");
        Person bob = person("dmbob7", ownerToken, workspaceId);
        Person carol = person("dmcarol7", ownerToken, workspaceId);
        String withBob = start(ownerToken, List.of(bob.id()), 200).get("id").asText();
        String withCarol = start(ownerToken, List.of(carol.id()), 200).get("id").asText();
        String bobMessageId = send(ownerToken, withBob, "hola bob", 201).get("id").asText();

        mockMvc.perform(post("/api/v1/conversations/" + withBob + "/messages")
                        .header("Authorization", "Bearer " + bob.token())
                        .contentType("application/json")
                        .content("{\"content\":\"respuesta\",\"parentMessageId\":\"" + bobMessageId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentMessageId").value(bobMessageId));

        mockMvc.perform(post("/api/v1/conversations/" + withCarol + "/messages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"content\":\"respuesta cruzada\",\"parentMessageId\":\"" + bobMessageId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void groupConversationsHaveAllTheirMembers() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm8@example.com", "dmowner8");
        String workspaceId = createWorkspace(ownerToken, "Group Team");
        Person bob = person("dmbob8", ownerToken, workspaceId);
        Person carol = person("dmcarol8", ownerToken, workspaceId);

        JsonNode group = start(ownerToken, List.of(bob.id(), carol.id()), 200);

        assertTrue(group.get("isGroup").asBoolean());
        assertEquals(3, group.get("memberIds").size());
        send(carol.token(), group.get("id").asText(), "hola grupo", 201);
    }

    @Test
    void receivingADirectMessage_createsANotificationForTheRecipientOnly() throws Exception {
        String ownerToken = registerAndGetAccessToken("dm9@example.com", "dmowner9");
        String workspaceId = createWorkspace(ownerToken, "Notify Team");
        Person bob = person("dmbob9", ownerToken, workspaceId);
        String conversationId = start(ownerToken, List.of(bob.id()), 200).get("id").asText();

        send(ownerToken, conversationId, "te aviso", 201);

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + bob.token()))
                .andExpect(jsonPath("$.content[0].type").value("DIRECT_MESSAGE"))
                .andExpect(jsonPath("$.content[0].conversationId").value(conversationId));
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void unknownConversation_isNotFound() throws Exception {
        String token = registerAndGetAccessToken("dm10@example.com", "dmowner10");

        mockMvc.perform(get("/api/v1/conversations/" + UUID.randomUUID() + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
