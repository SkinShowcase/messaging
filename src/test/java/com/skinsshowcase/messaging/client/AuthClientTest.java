package com.skinsshowcase.messaging.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthClientTest {

    private MockWebServer server;
    private AuthClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        var wc = WebClient.builder().baseUrl(server.url("/").toString()).build();
        client = new AuthClient(wc);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void getSteamIdByUsername_found() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody("{\"steamId\":\"76561198000000001\"}")
                .addHeader("Content-Type", "application/json"));

        var out = client.getSteamIdByUsername("bob", "Bearer t");

        assertThat(out).contains("76561198000000001");
        assertThat(server.takeRequest().getPath()).contains("/auth/users/by-username/bob");
    }

    @Test
    void getSteamIdByUsername_blankUsername_empty() {
        assertThat(client.getSteamIdByUsername("  ", null)).isEmpty();
    }

    @Test
    void batchPresetAvatarIds_emptyAuth_returnsEmpty() {
        assertThat(client.batchPresetAvatarIds(List.of("1"), null)).isEmpty();
    }

    @Test
    void batchPresetAvatarIds_ok() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody("{\"presetAvatarIdBySteamId\":{\"76561198000000001\":3}}")
                .addHeader("Content-Type", "application/json"));

        var map = client.batchPresetAvatarIds(List.of("76561198000000001"), "Bearer x");

        assertThat(map).containsEntry("76561198000000001", 3);
        assertThat(server.takeRequest().getPath()).isEqualTo("/auth/users/preset-avatar-ids");
    }
}
