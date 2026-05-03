package com.skinsshowcase.messaging.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionClientTest {

    private MockWebServer server;
    private AuthSessionClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        var wc = WebClient.builder().baseUrl(server.url("/").toString()).build();
        client = new AuthSessionClient(wc);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void blankHeader_unauthorized() {
        assertThat(client.checkSession("  ")).isEqualTo(AuthSessionClient.AuthSessionStatus.UNAUTHORIZED);
    }

    @Test
    void ok_204() {
        server.enqueue(new MockResponse().setResponseCode(204));
        assertThat(client.checkSession("Bearer t")).isEqualTo(AuthSessionClient.AuthSessionStatus.OK);
    }

    @Test
    void unauthorized_401() {
        server.enqueue(new MockResponse().setResponseCode(401));
        assertThat(client.checkSession("Bearer bad")).isEqualTo(AuthSessionClient.AuthSessionStatus.UNAUTHORIZED);
    }

    @Test
    void blocked_403() {
        server.enqueue(new MockResponse().setResponseCode(403));
        assertThat(client.checkSession("Bearer t")).isEqualTo(AuthSessionClient.AuthSessionStatus.BLOCKED);
    }

    @Test
    void otherHttp_error() {
        server.enqueue(new MockResponse().setResponseCode(500));
        assertThat(client.checkSession("Bearer t")).isEqualTo(AuthSessionClient.AuthSessionStatus.ERROR);
    }
}
