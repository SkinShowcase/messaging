package com.skinsshowcase.messaging.websocket;

import com.skinsshowcase.messaging.client.AuthSessionClient;
import com.skinsshowcase.messaging.config.TransportSecurityProperties;
import com.skinsshowcase.messaging.service.JwtSupportService;
import com.skinsshowcase.messaging.service.WebSocketSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * WebSocket handler для /ws/messages. Ожидает query-параметр token=JWT, извлекает steamId и регистрирует сессию.
 */
public class MessagingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingWebSocketHandler.class);
    private static final String TOKEN_PARAM = "token";

    private final JwtSupportService jwtSupportService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final AuthSessionClient authSessionClient;
    private final TransportSecurityProperties transportSecurityProperties;

    public MessagingWebSocketHandler(JwtSupportService jwtSupportService,
                                     WebSocketSessionRegistry sessionRegistry,
                                     AuthSessionClient authSessionClient,
                                     TransportSecurityProperties transportSecurityProperties) {
        this.jwtSupportService = jwtSupportService;
        this.sessionRegistry = sessionRegistry;
        this.authSessionClient = authSessionClient;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        if (transportSecurityProperties.isRequireSecureTransport() && !isSecureWebSocket(session)) {
            sendErrorAndClose(session, "Secure transport required (WSS)");
            return;
        }
        var token = getTokenFromSession(session);
        if (token == null || !jwtSupportService.isValid(token)) {
            sendErrorAndClose(session, "Missing or invalid token");
            return;
        }
        var sessionStatus = authSessionClient.checkSession("Bearer " + token);
        if (sessionStatus != AuthSessionClient.AuthSessionStatus.OK) {
            sendErrorAndClose(session, websocketSessionErrorMessage(sessionStatus));
            return;
        }
        var steamId = jwtSupportService.parseSubject(token);
        session.getAttributes().put(STEAM_ID_ATTR, steamId);
        sessionRegistry.register(steamId, session);
        log.debug("WebSocket registered for steamId={}", steamId);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        var steamId = (String) session.getAttributes().get(STEAM_ID_ATTR);
        if (steamId != null) {
            sessionRegistry.unregister(steamId);
        }
    }

    private static final String STEAM_ID_ATTR = "steamId";

    private static String getTokenFromSession(WebSocketSession session) {
        var uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        return parseQueryParam(uri.getQuery(), TOKEN_PARAM);
    }

    static String parseQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (var pair : query.split("&")) {
            var idx = pair.indexOf('=');
            if (idx > 0 && key.equals(pair.substring(0, idx).trim())) {
                var value = pair.substring(idx + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private static String websocketSessionErrorMessage(AuthSessionClient.AuthSessionStatus status) {
        if (status == AuthSessionClient.AuthSessionStatus.BLOCKED) {
            return "Account blocked";
        }
        if (status == AuthSessionClient.AuthSessionStatus.UNAUTHORIZED) {
            return "Session invalid";
        }
        return "Auth service unavailable";
    }

    private void sendErrorAndClose(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage("{\"error\":\"" + message + "\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.debug("Failed to send error and close: {}", e.getMessage());
        }
    }

    private static boolean isSecureWebSocket(WebSocketSession session) {
        var uri = session.getUri();
        if (uri != null && "wss".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        var proto = session.getHandshakeHeaders().getFirst("X-Forwarded-Proto");
        return proto != null && "https".equalsIgnoreCase(proto.trim());
    }
}
