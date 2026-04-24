package com.skinsshowcase.messaging.config;

import com.skinsshowcase.messaging.client.AuthSessionClient;
import com.skinsshowcase.messaging.service.JwtSupportService;
import com.skinsshowcase.messaging.service.WebSocketSessionRegistry;
import com.skinsshowcase.messaging.websocket.MessagingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtSupportService jwtSupportService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final AuthSessionClient authSessionClient;
    private final TransportSecurityProperties transportSecurityProperties;

    public WebSocketConfig(JwtSupportService jwtSupportService,
                           WebSocketSessionRegistry sessionRegistry,
                           AuthSessionClient authSessionClient,
                           TransportSecurityProperties transportSecurityProperties) {
        this.jwtSupportService = jwtSupportService;
        this.sessionRegistry = sessionRegistry;
        this.authSessionClient = authSessionClient;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messagingWebSocketHandler(), "/ws/messages")
                .setAllowedOrigins("*");
    }

    @Bean
    public MessagingWebSocketHandler messagingWebSocketHandler() {
        return new MessagingWebSocketHandler(jwtSupportService, sessionRegistry, authSessionClient, transportSecurityProperties);
    }
}
