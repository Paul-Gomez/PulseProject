package com.pulse.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class WebSocketIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pulse_ws_test")
            .withUsername("pulse")
            .withPassword("pulse");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("pulse.security.jwt.secret", () -> "test-secret-key-with-enough-length-1234567890");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void connectWithoutToken_isRejected() {
        assertThrows(Exception.class, () -> connect(null));
    }

    @Test
    void messageSentOverRest_arrivesInRealTimeToChannelSubscribers() throws Exception {
        String token = register("ws1@example.com", "wsuser1");
        String workspaceId = createWorkspace(token, "Realtime Team");
        String channelId = createChannel(token, workspaceId, "general", false);

        StompSession session = connect(token);
        BlockingQueue<String> events = subscribe(session, "/topic/channel." + channelId);

        post("/api/v1/channels/" + channelId + "/messages", token, Map.of("content", "hola en directo"));

        assertNotNull(awaitContaining(events, "hola en directo"));
        session.disconnect();
    }

    @Test
    void userOutsideTheWorkspace_doesNotReceiveChannelMessages() throws Exception {
        String ownerToken = register("ws2@example.com", "wsowner2");
        String outsiderToken = register("ws2b@example.com", "wsoutsider2");
        String workspaceId = createWorkspace(ownerToken, "Closed Team");
        String channelId = createChannel(ownerToken, workspaceId, "general", false);

        BlockingQueue<String> outsiderEvents = new LinkedBlockingQueue<>();
        try {
            StompSession outsider = connect(outsiderToken);
            outsiderEvents = subscribe(outsider, "/topic/channel." + channelId);
        } catch (Exception expected) {
            // The server may already have closed the session when rejecting the subscription.
        }

        StompSession owner = connect(ownerToken);
        subscribe(owner, "/topic/channel." + channelId);
        post("/api/v1/channels/" + channelId + "/messages", ownerToken, Map.of("content", "solo para el owner"));

        assertNull(outsiderEvents.poll(2, TimeUnit.SECONDS));
        owner.disconnect();
    }

    @Test
    void typingSignal_isBroadcastToTheChannel() throws Exception {
        String token = register("ws3@example.com", "wsuser3");
        String workspaceId = createWorkspace(token, "Typing Team");
        String channelId = createChannel(token, workspaceId, "general", false);

        StompSession session = connect(token);
        BlockingQueue<String> events = subscribe(session, "/topic/channel." + channelId);

        session.send("/app/channel." + channelId + "/typing", "");

        assertNotNull(awaitContaining(events, "TYPING"));
        session.disconnect();
    }

    @Test
    void presence_isBroadcastWhenAWorkspaceMemberConnectsAndDisconnects() throws Exception {
        String ownerToken = register("ws4@example.com", "wsowner4");
        String memberToken = register("ws4b@example.com", "wsmember4");
        String workspaceId = createWorkspace(ownerToken, "Presence Team");
        post("/api/v1/workspaces/" + workspaceId + "/members", ownerToken, Map.of("email", "ws4b@example.com"));
        String memberId = get("/api/v1/users/me", memberToken).get("id").asText();

        StompSession owner = connect(ownerToken);
        BlockingQueue<String> presence = subscribe(owner, "/topic/workspace." + workspaceId + ".presence");

        StompSession member = connect(memberToken);
        assertNotNull(awaitContaining(presence, "\"userId\":\"" + memberId + "\",\"status\":\"ONLINE\""));

        member.disconnect();
        assertNotNull(awaitContaining(presence, "\"userId\":\"" + memberId + "\",\"status\":\"OFFLINE\""));
        owner.disconnect();
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connectHeaders, new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<String> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((String) payload);
            }
        });
        // Subscriptions are registered asynchronously on the server; give it a moment before triggering events.
        Thread.sleep(500);
        return queue;
    }

    private String awaitContaining(BlockingQueue<String> queue, String text) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            String event = queue.poll(500, TimeUnit.MILLISECONDS);
            if (event != null && event.contains(text)) {
                return event;
            }
        }
        return null;
    }

    private String register(String email, String username) {
        JsonNode body = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "username", username, "password", "supersecret123", "displayName", username),
                JsonNode.class).getBody();
        return body.get("accessToken").asText();
    }

    private String createWorkspace(String token, String name) {
        return post("/api/v1/workspaces", token, Map.of("name", name)).get("id").asText();
    }

    private String createChannel(String token, String workspaceId, String name, boolean isPrivate) {
        JsonNode channel = post("/api/v1/workspaces/" + workspaceId + "/channels", token,
                Map.of("name", name, "type", "TEXT", "isPrivate", isPrivate));
        return channel.get("id").asText();
    }

    private JsonNode post(String url, String token, Object body) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, bearer(token)), JsonNode.class).getBody();
    }

    private JsonNode get(String url, String token) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class).getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
