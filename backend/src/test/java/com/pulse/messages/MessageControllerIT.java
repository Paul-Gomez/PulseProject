package com.pulse.messages;

import com.pulse.common.AbstractIntegrationTest;
import com.pulse.messages.dto.EditMessageRequest;
import com.pulse.messages.dto.ReactionRequest;
import com.pulse.messages.dto.SendMessageRequest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageControllerIT extends AbstractIntegrationTest {

    @Test
    void sendMessage_thenListIt_worksForWorkspaceMember() throws Exception {
        String token = registerAndGetAccessToken("msguser1@example.com", "msguser1");
        String workspaceId = createWorkspace(token, "Chatty Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Hola a todos", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hola a todos"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hola a todos"));
    }

    @Test
    void sendMessage_asOutsider_isForbidden() throws Exception {
        String ownerToken = registerAndGetAccessToken("msgowner2@example.com", "msgowner2");
        String outsiderToken = registerAndGetAccessToken("msgoutsider2@example.com", "msgoutsider2");
        String workspaceId = createWorkspace(ownerToken, "Closed Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");

        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Colandome", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void editOwnMessage_updatesContentAndSetsEditedAt() throws Exception {
        String token = registerAndGetAccessToken("msguser3@example.com", "msguser3");
        String workspaceId = createWorkspace(token, "Edit Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("mensaje original", null))))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(patch("/api/v1/messages/" + messageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new EditMessageRequest("mensaje editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("mensaje editado"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());
    }

    @Test
    void deleteMessage_asSomeoneElse_withoutPermission_isForbidden() throws Exception {
        String authorToken = registerAndGetAccessToken("msgauthor4@example.com", "msgauthor4");
        String otherToken = registerAndGetAccessToken("msgother4@example.com", "msgother4");
        String workspaceId = createWorkspace(authorToken, "Delete Team");
        String channelId = createPublicChannel(authorToken, workspaceId, "general");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType("application/json")
                        .content("{\"email\":\"msgother4@example.com\"}"))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("no me borres", null))))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/v1/messages/" + messageId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactToMessage_thenRemoveReaction_doesNotFail() throws Exception {
        String token = registerAndGetAccessToken("msguser5@example.com", "msguser5");
        String workspaceId = createWorkspace(token, "Reaction Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("reacciona a esto", null))))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/reactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReactionRequest("👍"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/messages/" + messageId + "/reactions/👍")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
