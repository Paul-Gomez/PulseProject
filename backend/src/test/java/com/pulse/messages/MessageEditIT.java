package com.pulse.messages;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageEditIT extends AbstractIntegrationTest {

    private String sendMessage(String token, String channelId, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void editMessageOfAnotherUser_isForbidden() throws Exception {
        String authorToken = registerAndGetAccessToken("edit1@example.com", "edit1");
        String otherToken = registerAndGetAccessToken("edit1b@example.com", "edit1b");
        String workspaceId = createWorkspace(authorToken, "Edit Rules");
        String channelId = createPublicChannel(authorToken, workspaceId, "general");

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType("application/json")
                        .content("{\"email\":\"edit1b@example.com\"}"))
                .andExpect(status().isCreated());

        String messageId = sendMessage(authorToken, channelId, "mensaje original");

        mockMvc.perform(patch("/api/v1/messages/" + messageId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType("application/json")
                        .content("{\"content\":\"te lo cambio\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editDeletedMessage_returnsNotFound() throws Exception {
        String token = registerAndGetAccessToken("edit2@example.com", "edit2");
        String workspaceId = createWorkspace(token, "Edit Deleted");
        String channelId = createPublicChannel(token, workspaceId, "general");
        String messageId = sendMessage(token, channelId, "voy a borrar esto");

        mockMvc.perform(delete("/api/v1/messages/" + messageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/messages/" + messageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"intento editar\"}"))
                .andExpect(status().isNotFound());
    }
}
