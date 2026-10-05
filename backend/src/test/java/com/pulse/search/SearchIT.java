package com.pulse.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchIT extends AbstractIntegrationTest {

    private String send(String token, String channelId, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private JsonNode search(String token, String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/search/messages?" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void invite(String ownerToken, String workspaceId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void textSearch_isCaseInsensitive_matchesPartialWords_andIgnoresOtherMessages() throws Exception {
        String token = registerAndGetAccessToken("search1@example.com", "searchuser1");
        String workspaceId = createWorkspace(token, "Search Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        send(token, channelId, "Estoy aprendiendo Docker compose");
        send(token, channelId, "Hoy toca estudiar Kubernetes");

        JsonNode results = search(token, "q=dock&workspaceId=" + workspaceId);

        assertEquals(1, results.get("totalElements").asInt());
        assertEquals("Estoy aprendiendo Docker compose", results.get("content").get(0).get("content").asText());
        assertEquals("general", results.get("content").get(0).get("channelName").asText());
    }

    @Test
    void searchEscapesLikeWildcards_soPercentAndUnderscoreAreLiteral() throws Exception {
        String token = registerAndGetAccessToken("search2@example.com", "searchuser2");
        String workspaceId = createWorkspace(token, "Wildcards Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        send(token, channelId, "descuento del 100% hoy");
        send(token, channelId, "descuento de 1000 euros");

        assertEquals(1, search(token, "q=100%25&workspaceId=" + workspaceId).get("totalElements").asInt());
        assertEquals(0, search(token, "q=%25%25%25&workspaceId=" + workspaceId).get("totalElements").asInt());
    }

    @Test
    void filtersByChannelAuthorAndDateRange() throws Exception {
        String ownerToken = registerAndGetAccessToken("search3@example.com", "searchowner3");
        String memberToken = registerAndGetAccessToken("search3b@example.com", "searchmember3");
        String workspaceId = createWorkspace(ownerToken, "Filters Team");
        String general = createPublicChannel(ownerToken, workspaceId, "general");
        String random = createPublicChannel(ownerToken, workspaceId, "random");
        invite(ownerToken, workspaceId, "search3b@example.com");
        send(ownerToken, general, "mensaje del owner en general");
        send(memberToken, general, "mensaje del miembro en general");
        send(ownerToken, random, "mensaje del owner en random");

        assertEquals(2, search(ownerToken, "workspaceId=" + workspaceId + "&channelId=" + general).get("totalElements").asInt());
        assertEquals(2, search(ownerToken, "workspaceId=" + workspaceId + "&authorId=" + userId(ownerToken)).get("totalElements").asInt());
        assertEquals(1, search(ownerToken, "workspaceId=" + workspaceId + "&authorId=" + userId(memberToken)).get("totalElements").asInt());
        assertEquals(3, search(ownerToken, "workspaceId=" + workspaceId + "&from=2020-01-01T00:00:00Z").get("totalElements").asInt());
        assertEquals(0, search(ownerToken, "workspaceId=" + workspaceId + "&from=2999-01-01T00:00:00Z").get("totalElements").asInt());
        assertEquals(0, search(ownerToken, "workspaceId=" + workspaceId + "&to=2020-01-01T00:00:00Z").get("totalElements").asInt());
    }

    @Test
    void resultsArePaginatedAndSortable() throws Exception {
        String token = registerAndGetAccessToken("search4@example.com", "searchuser4");
        String workspaceId = createWorkspace(token, "Pages Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        for (int i = 1; i <= 5; i++) {
            send(token, channelId, "mensaje numero " + i);
        }

        JsonNode firstPage = search(token, "workspaceId=" + workspaceId + "&size=2&page=0&sort=createdAt,asc");
        assertEquals(5, firstPage.get("totalElements").asInt());
        assertEquals(3, firstPage.get("totalPages").asInt());
        assertEquals(2, firstPage.get("content").size());
        assertEquals("mensaje numero 1", firstPage.get("content").get(0).get("content").asText());

        JsonNode lastPage = search(token, "workspaceId=" + workspaceId + "&size=2&page=2&sort=createdAt,desc");
        assertEquals(1, lastPage.get("content").size());
        assertEquals("mensaje numero 1", lastPage.get("content").get(0).get("content").asText());
    }

    @Test
    void neverReturnsMessagesTheUserCannotSee() throws Exception {
        String ownerToken = registerAndGetAccessToken("search5@example.com", "searchowner5");
        String memberToken = registerAndGetAccessToken("search5b@example.com", "searchmember5");
        String outsiderToken = registerAndGetAccessToken("search5c@example.com", "searchoutsider5");
        String workspaceId = createWorkspace(ownerToken, "Visibility Team");
        String publicChannel = createPublicChannel(ownerToken, workspaceId, "general");
        String privateChannel = createChannel(ownerToken, workspaceId, "secreto", true);
        invite(ownerToken, workspaceId, "search5b@example.com");
        send(ownerToken, publicChannel, "visibilidad publica");
        send(ownerToken, privateChannel, "visibilidad privada");

        assertEquals(2, search(ownerToken, "q=visibilidad").get("totalElements").asInt());
        assertEquals(1, search(memberToken, "q=visibilidad").get("totalElements").asInt());
        assertEquals(0, search(outsiderToken, "q=visibilidad").get("totalElements").asInt());
    }

    @Test
    void deletedMessagesAreNotFound() throws Exception {
        String token = registerAndGetAccessToken("search6@example.com", "searchuser6");
        String workspaceId = createWorkspace(token, "Deleted Team");
        String channelId = createPublicChannel(token, workspaceId, "general");
        String messageId = send(token, channelId, "mensaje que sera borrado");

        mockMvc.perform(delete("/api/v1/messages/" + messageId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(0, search(token, "q=borrado").get("totalElements").asInt());
    }

    @Test
    void mentionsCanBeFoundByTheMentionedUser() throws Exception {
        String ownerToken = registerAndGetAccessToken("search7@example.com", "searchowner7");
        String memberToken = registerAndGetAccessToken("search7b@example.com", "searchmember7");
        String workspaceId = createWorkspace(ownerToken, "Mentions Search");
        String channelId = createPublicChannel(ownerToken, workspaceId, "general");
        invite(ownerToken, workspaceId, "search7b@example.com");
        send(ownerToken, channelId, "hola @searchmember7 mira esto");
        send(ownerToken, channelId, "este no menciona a nadie");

        JsonNode results = search(memberToken, "mentionedUserId=" + userId(memberToken));

        assertEquals(1, results.get("totalElements").asInt());
    }

    @Test
    void sortingByAnythingElseIsRejected_andSearchRequiresLogin() throws Exception {
        String token = registerAndGetAccessToken("search8@example.com", "searchuser8");

        mockMvc.perform(get("/api/v1/search/messages?sort=content,asc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search/messages")).andExpect(status().isUnauthorized());
    }
}
