package com.pulse.workspaces;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceBanIT extends AbstractIntegrationTest {

    private String ownerToken;
    private String adminToken;
    private String moderatorToken;
    private String memberToken;
    private String workspaceId;
    private String adminId;
    private String memberId;
    private String memberEmail;

    @BeforeEach
    void createWorkspace() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        memberEmail = "bmem" + suffix + "@example.com";
        ownerToken = registerAndGetAccessToken("bown" + suffix + "@example.com", "bown" + suffix);
        adminToken = registerAndGetAccessToken("badm" + suffix + "@example.com", "badm" + suffix);
        moderatorToken = registerAndGetAccessToken("bmod" + suffix + "@example.com", "bmod" + suffix);
        memberToken = registerAndGetAccessToken(memberEmail, "bmem" + suffix);
        workspaceId = createWorkspace(ownerToken, "Bans " + suffix);

        invite("badm" + suffix + "@example.com");
        invite("bmod" + suffix + "@example.com");
        invite(memberEmail);
        adminId = userId(adminToken);
        memberId = userId(memberToken);
        changeRole(adminId, "ADMIN");
        changeRole(userId(moderatorToken), "MODERATOR");
    }

    private void invite(String email) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
    }

    private void changeRole(String userId, String role) throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId + "/members/" + userId + "/role")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"roleName\":\"" + role + "\"}"))
                .andExpect(status().isOk());
    }

    private String userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void ban(String token, String targetUserId, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/bans/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reason\":\"spam\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void bannedUser_isRemovedAndLosesAccessToTheWorkspace() throws Exception {
        ban(moderatorToken, memberId, 201);

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId).header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/bans").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(memberId))
                .andExpect(jsonPath("$.content[0].reason").value("spam"));
    }

    @Test
    void bannedUser_cannotBeInvitedAgainUntilUnbanned() throws Exception {
        ban(adminToken, memberId, 201);

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + memberEmail + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId + "/bans/" + memberId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        invite(memberEmail);
    }

    @Test
    void banningTwiceIsAConflict() throws Exception {
        ban(adminToken, memberId, 201);
        ban(adminToken, memberId, 409);
    }

    @Test
    void moderatorCannotBanAnAdminOrTheOwner_andMembersCannotBanAnyone() throws Exception {
        ban(moderatorToken, adminId, 403);
        ban(moderatorToken, userId(ownerToken), 403);
        ban(memberToken, adminId, 403);
    }

    @Test
    void banningSomeoneWhoDoesNotExist_returnsNotFound() throws Exception {
        ban(adminToken, java.util.UUID.randomUUID().toString(), 404);
    }
}
