package com.pulse.workspaces;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleHierarchyIT extends AbstractIntegrationTest {

    private String ownerToken;
    private String adminToken;
    private String moderatorToken;
    private String memberToken;
    private String workspaceId;
    private String ownerId;
    private String adminId;
    private String memberId;

    @BeforeEach
    void createWorkspaceWithOneUserPerRole() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        ownerToken = registerAndGetAccessToken("hown" + suffix + "@example.com", "hown" + suffix);
        adminToken = registerAndGetAccessToken("hadm" + suffix + "@example.com", "hadm" + suffix);
        moderatorToken = registerAndGetAccessToken("hmod" + suffix + "@example.com", "hmod" + suffix);
        memberToken = registerAndGetAccessToken("hmem" + suffix + "@example.com", "hmem" + suffix);
        workspaceId = createWorkspace(ownerToken, "Hierarchy " + suffix);

        invite("hadm" + suffix + "@example.com");
        invite("hmod" + suffix + "@example.com");
        invite("hmem" + suffix + "@example.com");

        ownerId = userId(ownerToken);
        adminId = userId(adminToken);
        memberId = userId(memberToken);
        changeRole(ownerToken, adminId, "ADMIN", 200);
        changeRole(ownerToken, userId(moderatorToken), "MODERATOR", 200);
    }

    private void invite(String email) throws Exception {
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

    private void changeRole(String token, String targetUserId, String role, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId + "/members/" + targetUserId + "/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"roleName\":\"" + role + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void remove(String token, String targetUserId, int expectedStatus) throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId + "/members/" + targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void moderatorCannotRemoveAnAdminNorTheOwner_butCanRemoveAMember() throws Exception {
        remove(moderatorToken, adminId, 403);
        remove(moderatorToken, ownerId, 403);
        remove(moderatorToken, memberId, 204);
    }

    @Test
    void adminCannotTouchTheOwner() throws Exception {
        remove(adminToken, ownerId, 403);
        changeRole(adminToken, ownerId, "MEMBER", 403);
    }

    @Test
    void adminCannotAssignOwnerOrAdminRoles_butCanAssignLowerOnes() throws Exception {
        changeRole(adminToken, memberId, "OWNER", 403);
        changeRole(adminToken, memberId, "ADMIN", 403);
        changeRole(adminToken, memberId, "MODERATOR", 200);
    }

    @Test
    void nobodyCanChangeTheirOwnRole() throws Exception {
        changeRole(adminToken, adminId, "OWNER", 403);
        changeRole(ownerToken, ownerId, "MEMBER", 403);
    }

    @Test
    void ownerCanPromoteAndRemoveAnyoneElse() throws Exception {
        changeRole(ownerToken, memberId, "ADMIN", 200);
        remove(ownerToken, adminId, 204);
    }
}
