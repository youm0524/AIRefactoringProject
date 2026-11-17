package com.careerpick.websocket;

import com.careerpick.repository.CommunityRoomMemberRepository;
import com.careerpick.service.AuthService;
import com.careerpick.service.AuthService.AuthenticatedSession;
import com.careerpick.service.CommunityMessageService;
import com.careerpick.service.CommunityMessageService.StoredMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class CommunityWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(CommunityWebSocketHandler.class);
    private static final CloseStatus TRY_AGAIN_LATER = new CloseStatus(1013, "Try Again Later");
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final CommunityMessageService messageService;
    private final CommunityRoomMemberRepository members;
    private final TaskScheduler scheduler;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public CommunityWebSocketHandler(ObjectMapper objectMapper, AuthService authService,
                                     CommunityMessageService messageService,
                                     CommunityRoomMemberRepository members, TaskScheduler scheduler) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.messageService = messageService;
        this.members = members;
        this.scheduler = scheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(2048);
        Connection connection = new Connection(session);
        connections.put(session.getId(), connection);
        connection.authTimeout = scheduler.schedule(
            () -> closeUnauthenticated(connection), Instant.now().plusSeconds(5));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage payload) {
        Connection connection = connections.get(session.getId());
        if (connection == null) return;

        JsonNode frame;
        try {
            frame = objectMapper.readTree(payload.getPayload());
        } catch (Exception exception) {
            boolean unauthenticated = connection.auth == null;
            fail(connection, unauthenticated ? "AUTH_REQUIRED" : "VALIDATION_FAILED",
                unauthenticated ? "먼저 인증해야 합니다." : "올바른 JSON 메시지가 아닙니다.",
                null, unauthenticated);
            return;
        }

        String type = text(frame, "type");
        if (connection.auth == null) {
            if (!"AUTH".equals(type)) {
                fail(connection, "AUTH_REQUIRED", "먼저 인증해야 합니다.", null, true);
                return;
            }
            authenticate(connection, text(frame, "token"));
            return;
        }

        if (!"SEND_MESSAGE".equals(type)) {
            fail(connection, "UNSUPPORTED_TYPE", "지원하지 않는 메시지 타입입니다.", null, false);
            return;
        }
        sendMessage(connection, frame);
    }

    private void authenticate(Connection connection, String token) {
        if (token == null || token.isBlank()) {
            fail(connection, "AUTH_REQUIRED", "로그인이 필요합니다.", null, true);
            return;
        }
        try {
            connection.auth = authService.requireWebSocketSession(token);
            cancel(connection.authTimeout);
            connection.expiry = scheduler.schedule(() -> expire(connection), connection.auth.expiresAt());
            send(connection.session, Map.of(
                "type", "AUTHENTICATED",
                "expiresAt", connection.auth.expiresAt().toString()));
        } catch (ResponseStatusException exception) {
            fail(connection, "AUTH_REQUIRED", "로그인이 만료되었거나 올바르지 않습니다.", null, true);
        } catch (DataAccessException exception) {
            fail(connection, "SERVICE_UNAVAILABLE", "인증 서비스를 사용할 수 없습니다.", null, TRY_AGAIN_LATER);
        }
    }

    private void sendMessage(Connection connection, JsonNode frame) {
        String clientMessageId = text(frame, "clientMessageId");
        Long roomId = positiveLong(frame, "roomId");
        String messageText = text(frame, "text");
        String canonicalId;
        try {
            canonicalId = UUID.fromString(clientMessageId).toString();
        } catch (Exception exception) {
            fail(connection, "VALIDATION_FAILED", "clientMessageId가 올바르지 않습니다.", clientMessageId, false);
            return;
        }
        if (roomId == null || messageText == null || messageText.trim().isEmpty() || messageText.trim().length() > 120) {
            fail(connection, "VALIDATION_FAILED", "방 ID와 1~120자의 메시지를 확인해 주세요.", canonicalId, false);
            return;
        }

        try {
            AuthenticatedSession current = authService.requireWebSocketSession(connection.auth.token());
            StoredMessage stored = messageService.store(
                current.userId(), roomId, canonicalId, messageText.trim());
            boolean published = broadcast(stored);
            send(connection.session, sendResult(stored, published));
        } catch (ResponseStatusException exception) {
            int status = exception.getStatusCode().value();
            String code = status == 403 ? "NOT_A_MEMBER" : "AUTH_REQUIRED";
            fail(connection, code, status == 403 ? "참여 중인 채팅방이 아닙니다." : "로그인이 만료되었습니다.",
                canonicalId, status == 401);
        } catch (DataAccessException exception) {
            log.error("Failed to store or deliver a community message", exception);
            fail(connection, "MESSAGE_STORE_FAILED", "메시지를 저장하지 못했습니다.", canonicalId, false);
        } catch (RuntimeException exception) {
            log.error("Unexpected community message failure", exception);
            fail(connection, "MESSAGE_STORE_FAILED", "메시지를 저장하지 못했습니다.", canonicalId, false);
        }
    }

    private boolean broadcast(StoredMessage message) {
        ArrayList<Connection> authenticated = connections.values().stream()
            .filter(connection -> connection.auth != null && connection.session.isOpen())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        if (authenticated.isEmpty()) return true;

        Set<Long> connectedUserIds = authenticated.stream()
            .map(connection -> connection.auth.userId())
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> recipients = members.findUserIdsByRoomIdAndUserIdIn(message.roomId(), connectedUserIds);
        boolean delivered = true;
        for (Connection connection : authenticated) {
            if (!recipients.contains(connection.auth.userId())) continue;
            delivered &= send(connection.session, Map.of(
                "type", "MESSAGE_CREATED",
                "message", messageView(message, connection.auth.userId())));
        }
        return delivered;
    }

    private Map<String, Object> sendResult(StoredMessage message, boolean published) {
        return Map.of(
            "type", "SEND_RESULT",
            "clientMessageId", message.clientMessageId(),
            "status", published ? "PUBLISHED" : "STORED_BUT_NOT_BROADCAST",
            "message", messageView(message, message.userId()));
    }

    private Map<String, Object> messageView(StoredMessage message, long viewerId) {
        return Map.of(
            "id", message.id(),
            "roomId", message.roomId(),
            "author", message.author(),
            "text", message.text(),
            "createdAt", message.createdAt().toString(),
            "mine", message.userId() == viewerId,
            "clientMessageId", message.clientMessageId());
    }

    private void closeUnauthenticated(Connection connection) {
        if (connection.auth == null) {
            fail(connection, "AUTH_REQUIRED", "인증 시간이 초과되었습니다.", null, true);
        }
    }

    private void expire(Connection connection) {
        fail(connection, "SESSION_EXPIRED", "로그인이 만료되었습니다.", null, true);
    }

    private void fail(Connection connection, String code, String message, String clientMessageId, boolean close) {
        fail(connection, code, message, clientMessageId,
            close ? CloseStatus.POLICY_VIOLATION : null);
    }

    private void fail(Connection connection, String code, String message, String clientMessageId,
                      CloseStatus closeStatus) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "ERROR");
        frame.put("code", code);
        frame.put("message", message);
        if (clientMessageId != null) frame.put("clientMessageId", clientMessageId);
        send(connection.session, frame);
        if (closeStatus != null) close(connection.session, closeStatus);
    }

    private boolean send(WebSocketSession session, Object frame) {
        if (!session.isOpen()) return false;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
            }
            return true;
        } catch (IOException exception) {
            close(session, CloseStatus.SERVER_ERROR);
            return false;
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (IOException ignored) {
            // The connection is already unusable.
        } finally {
            remove(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        close(session, CloseStatus.SERVER_ERROR);
    }

    private void remove(String sessionId) {
        Connection removed = connections.remove(sessionId);
        if (removed == null) return;
        cancel(removed.authTimeout);
        cancel(removed.expiry);
    }

    private void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    private String text(JsonNode frame, String field) {
        JsonNode value = frame.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private Long positiveLong(JsonNode frame, String field) {
        JsonNode value = frame.get(field);
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0
            ? value.longValue() : null;
    }

    private static final class Connection {
        private final WebSocketSession session;
        private volatile AuthenticatedSession auth;
        private volatile ScheduledFuture<?> authTimeout;
        private volatile ScheduledFuture<?> expiry;

        private Connection(WebSocketSession session) {
            this.session = session;
        }
    }
}
