package com.pulse.identity;

import com.pulse.common.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitIT extends AbstractIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void rateLimitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("pulse.rate-limit.enabled", () -> "true");
        registry.add("pulse.rate-limit.auth-requests-per-minute", () -> "5");
        registry.add("pulse.rate-limit.max-failed-logins", () -> "3");
    }

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void cleanCounters() {
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    private void login(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void repeatedWrongPasswords_lockTheAccountEvenForTheCorrectPassword() throws Exception {
        registerAndGetAccessToken("lock1@example.com", "lock1");

        login("lock1@example.com", "incorrecta1", 401);
        login("lock1@example.com", "incorrecta2", 401);
        login("lock1@example.com", "incorrecta3", 401);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"lock1@example.com\",\"password\":\"supersecret123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    @Test
    void successfulLogin_resetsTheFailedAttemptCounter() throws Exception {
        registerAndGetAccessToken("lock2@example.com", "lock2");

        login("lock2@example.com", "incorrecta1", 401);
        login("lock2@example.com", "incorrecta2", 401);
        login("lock2@example.com", "supersecret123", 200);
        login("lock2@example.com", "incorrecta3", 401);
        login("lock2@example.com", "incorrecta4", 401);
    }

    @Test
    void tooManyLoginRequestsFromTheSameIp_areRateLimited() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("nadie" + i + "@example.com", "whatever123", 401);
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nadie6@example.com\",\"password\":\"whatever123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void registrationIsRateLimitedToo() throws Exception {
        for (int i = 1; i <= 5; i++) {
            registerAndGetAccessToken("bulk" + i + "@example.com", "bulkuser" + i);
        }

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"bulk6@example.com\",\"username\":\"bulkuser6\","
                                + "\"password\":\"supersecret123\",\"displayName\":\"bulk6\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
