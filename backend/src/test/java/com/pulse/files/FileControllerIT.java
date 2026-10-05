package com.pulse.files;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulse.common.AbstractIntegrationTest;
import com.pulse.files.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "pulse.storage.max-file-size-bytes=1024")
class FileControllerIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class InMemoryStorageConfig {
        @Bean
        @Primary
        StorageService inMemoryStorageService() {
            return new InMemoryStorageService();
        }
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    }

    private JsonNode upload(String token, String channelId, byte[] content, String declaredType, int expectedStatus)
            throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/channels/" + channelId + "/attachments")
                        .file(new MockMultipartFile("file", "foto.png", declaredType, content))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void inviteMember(String ownerToken, String workspaceId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
    }

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String sendMessageWithAttachment(String token, String channelId, String attachmentId, int expectedStatus)
            throws Exception {
        return mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"mira esto\",\"attachmentIds\":[\"" + attachmentId + "\"]}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void uploadedFile_attachedToAMessage_canBeDownloadedByChannelMembers() throws Exception {
        String token = registerAndGetAccessToken("file1@example.com", "file1");
        String workspaceId = createWorkspace(token, "Files Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        byte[] png = pngBytes();

        String attachmentId = upload(token, channelId, png, "image/png", 201).get("id").asText();

        mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"mira esto\",\"attachmentIds\":[\"" + attachmentId + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0].fileName").value("foto.png"))
                .andExpect(jsonPath("$.attachments[0].mimeType").value("image/png"));

        byte[] downloaded = mockMvc.perform(get("/api/v1/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(png, downloaded);
    }

    @Test
    void fileWhoseContentDoesNotMatchItsDeclaredType_isRejected() throws Exception {
        String token = registerAndGetAccessToken("file2@example.com", "file2");
        String workspaceId = createWorkspace(token, "Strict Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        byte[] executableLikeBytes = {'M', 'Z', 0, 0, 1, 2, 3};
        upload(token, channelId, executableLikeBytes, "image/png", 415);

        byte[] plainText = "esto es solo texto".getBytes();
        upload(token, channelId, plainText, "image/png", 415);

        upload(token, channelId, pngBytes(), "application/x-msdownload", 415);
    }

    @Test
    void fileBiggerThanTheLimit_isRejected() throws Exception {
        String token = registerAndGetAccessToken("file3@example.com", "file3");
        String workspaceId = createWorkspace(token, "Limits Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        byte[] tooBig = new byte[2000];
        System.arraycopy(pngBytes(), 0, tooBig, 0, 8);

        mockMvc.perform(multipart("/api/v1/channels/" + channelId + "/attachments")
                        .file(new MockMultipartFile("file", "grande.png", "image/png", tooBig))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    void userOutsideTheWorkspace_cannotUploadNorDownload() throws Exception {
        String ownerToken = registerAndGetAccessToken("file4@example.com", "file4");
        String outsiderToken = registerAndGetAccessToken("file4b@example.com", "file4b");
        String workspaceId = createWorkspace(ownerToken, "Closed Files");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");

        String attachmentId = upload(ownerToken, channelId, pngBytes(), "image/png", 201).get("id").asText();
        sendMessageWithAttachment(ownerToken, channelId, attachmentId, 201);

        mockMvc.perform(get("/api/v1/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        upload(outsiderToken, channelId, pngBytes(), "image/png", 403);
    }

    @Test
    void guestsCannotUploadFiles() throws Exception {
        String ownerToken = registerAndGetAccessToken("file5@example.com", "file5");
        String guestToken = registerAndGetAccessToken("file5b@example.com", "file5b");
        String workspaceId = createWorkspace(ownerToken, "Guests Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        inviteMember(ownerToken, workspaceId, "file5b@example.com");

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId + "/members/" + userId(guestToken) + "/role")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"roleName\":\"GUEST\"}"))
                .andExpect(status().isOk());

        upload(guestToken, channelId, pngBytes(), "image/png", 403);
    }

    @Test
    void fileNotYetSentWithAMessage_isOnlyVisibleToWhoUploadedIt() throws Exception {
        String ownerToken = registerAndGetAccessToken("file6@example.com", "file6");
        String memberToken = registerAndGetAccessToken("file6b@example.com", "file6b");
        String workspaceId = createWorkspace(ownerToken, "Drafts Team");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        inviteMember(ownerToken, workspaceId, "file6b@example.com");

        String attachmentId = upload(ownerToken, channelId, pngBytes(), "image/png", 201).get("id").asText();

        mockMvc.perform(get("/api/v1/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void cannotAttachAFileUploadedBySomeoneElse() throws Exception {
        String ownerToken = registerAndGetAccessToken("file7@example.com", "file7");
        String memberToken = registerAndGetAccessToken("file7b@example.com", "file7b");
        String workspaceId = createWorkspace(ownerToken, "Stolen Files");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        inviteMember(ownerToken, workspaceId, "file7b@example.com");

        String attachmentId = upload(ownerToken, channelId, pngBytes(), "image/png", 201).get("id").asText();

        sendMessageWithAttachment(memberToken, channelId, attachmentId, 404);
    }

    @Test
    void deletingTheMessage_removesItsFiles() throws Exception {
        String token = registerAndGetAccessToken("file8@example.com", "file8");
        String workspaceId = createWorkspace(token, "Cleanup Team");
        String channelId = createPublicChannel(token, workspaceId, "general");

        String attachmentId = upload(token, channelId, pngBytes(), "image/png", 201).get("id").asText();
        String messageBody = sendMessageWithAttachment(token, channelId, attachmentId, 201);
        String messageId = objectMapper.readTree(messageBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/messages/" + messageId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
