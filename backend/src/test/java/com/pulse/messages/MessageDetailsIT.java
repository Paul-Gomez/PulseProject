package com.pulse.messages;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What a client needs to draw the chat without extra calls: reactions on each message and member profiles. */
class MessageDetailsIT extends AbstractIntegrationTest {

    @Test
    void listedMessagesIncludeTheirReactions() throws Exception {
        String token = registerAndGetAccessToken("details1@example.com", "details1");
        String workspaceId = createWorkspace(token, "Details Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"reacciona\"}"))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(post("/api/v1/messages/" + messageId + "/reactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"emoji\":\"🎉\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/channels/" + channelId + "/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reactions[0].emoji").value("🎉"))
                .andExpect(jsonPath("$.content[0].reactions[0].count").value(1));
    }

    @Test
    void memberListIncludesEachMembersPublicProfile() throws Exception {
        String token = registerAndGetAccessToken("details2@example.com", "details2");
        String workspaceId = createWorkspace(token, "Profiles Team");

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/members").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].user.username").value("details2"))
                .andExpect(jsonPath("$.content[0].user.displayName").value("details2"))
                .andExpect(jsonPath("$.content[0].user.email").doesNotExist());
    }
}
