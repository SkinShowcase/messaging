package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.client.AuthSessionClient;
import com.skinsshowcase.messaging.config.MessagingTcpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP-сервер на отдельном порту. Клиент подключается, отправляет первую строку — JWT.
 * После успешной валидации получает уведомления в формате одной строки на событие:
 * NEW_MESSAGE &lt;senderSteamId&gt; &lt;messageId&gt;
 */
@Component
public class TcpNotificationServer {

    private static final Logger log = LoggerFactory.getLogger(TcpNotificationServer.class);
    private static final String CMD_PREFIX = "NEW_MESSAGE ";
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    private final MessagingTcpProperties tcpProperties;
    private final JwtSupportService jwtSupportService;
    private final AuthSessionClient authSessionClient;
    private final Map<String, List<ClientConnection>> connectionsBySteamId = new ConcurrentHashMap<>();
    private volatile boolean running;
    private Thread acceptThread;

    public TcpNotificationServer(MessagingTcpProperties tcpProperties,
                                 JwtSupportService jwtSupportService,
                                 AuthSessionClient authSessionClient) {
        this.tcpProperties = tcpProperties;
        this.jwtSupportService = jwtSupportService;
        this.authSessionClient = authSessionClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        running = true;
        acceptThread = new Thread(this::acceptLoop, "tcp-notification-accept");
        acceptThread.setDaemon(false);
        acceptThread.start();
        log.info("TCP notification server started on port {}", tcpProperties.getPort());
    }

    public void notifyNewMessage(String recipientSteamId, String senderSteamId, UUID messageId) {
        var line = CMD_PREFIX + senderSteamId + " " + messageId + "\n";
        var list = connectionsBySteamId.get(recipientSteamId);
        if (list == null) {
            return;
        }
        var toRemove = new ArrayList<ClientConnection>();
        for (var conn : list) {
            if (!conn.send(line)) {
                toRemove.add(conn);
            }
        }
        toRemove.forEach(this::unregister);
    }

    private void acceptLoop() {
        try (var serverSocket = new ServerSocket(tcpProperties.getPort())) {
            while (running) {
                try {
                    var socket = serverSocket.accept();
                    handleConnection(socket);
                } catch (IOException e) {
                    if (running) {
                        log.warn("Accept error: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("TCP server failed: {}", e.getMessage());
        }
    }

    private void handleConnection(Socket socket) {
        var thread = new Thread(() -> processClient(socket), "tcp-client-" + socket.getRemoteSocketAddress());
        thread.setDaemon(true);
        thread.start();
    }

    private void processClient(Socket socket) {
        String steamId = null;
        ClientConnection client = null;
        try {
            socket.setSoTimeout(30_000);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF8));
            var firstLine = reader.readLine();
            if (firstLine == null || firstLine.isBlank()) {
                closeSocket(socket);
                return;
            }
            var token = firstLine.trim();
            if (!jwtSupportService.isValid(token)) {
                sendLine(socket, "ERROR Invalid token");
                closeSocket(socket);
                return;
            }
            var sessionStatus = authSessionClient.checkSession("Bearer " + token);
            if (sessionStatus != AuthSessionClient.AuthSessionStatus.OK) {
                sendLine(socket, tcpSessionErrorLine(sessionStatus));
                closeSocket(socket);
                return;
            }
            steamId = jwtSupportService.parseSubject(token);
            client = new ClientConnection(socket, steamId);
            register(client);
            sendLine(socket, "OK " + steamId);
            while (running && socket.isConnected()) {
                var line = reader.readLine();
                if (line == null) {
                    break;
                }
                if ("PING".equals(line.trim())) {
                    sendLine(socket, "PONG");
                }
            }
        } catch (Exception e) {
            log.debug("TCP client disconnected: {}", e.getMessage());
        } finally {
            if (client != null) {
                unregister(client);
            }
            closeSocket(socket);
        }
    }

    private void register(ClientConnection client) {
        connectionsBySteamId.compute(client.steamId, (k, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(client);
            return list;
        });
    }

    private void unregister(ClientConnection client) {
        connectionsBySteamId.computeIfPresent(client.steamId, (k, list) -> {
            list.remove(client);
            return list.isEmpty() ? null : list;
        });
    }

    private static String tcpSessionErrorLine(AuthSessionClient.AuthSessionStatus status) {
        if (status == AuthSessionClient.AuthSessionStatus.BLOCKED) {
            return "ERROR Account blocked";
        }
        if (status == AuthSessionClient.AuthSessionStatus.UNAUTHORIZED) {
            return "ERROR Invalid token";
        }
        return "ERROR Auth unavailable";
    }

    private static void sendLine(Socket socket, String line) {
        try {
            var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF8), true);
            out.println(line);
        } catch (IOException e) {
            log.debug("Send to TCP client failed: {}", e.getMessage());
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            log.trace("Close socket: {}", e.getMessage());
        }
    }

    private static final class ClientConnection {
        private final Socket socket;
        private final String steamId;
        private final PrintWriter writer;

        ClientConnection(Socket socket, String steamId) throws IOException {
            this.socket = socket;
            this.steamId = steamId;
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF8), true);
        }

        boolean send(String line) {
            try {
                if (!socket.isClosed()) {
                    writer.print(line);
                    writer.flush();
                    return true;
                }
            } catch (Exception e) {
                // fall through
            }
            return false;
        }
    }
}
