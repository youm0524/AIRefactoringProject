package com.careerpick.websocket;

import com.careerpick.service.AuthService;
import com.careerpick.service.AuthService.AuthenticatedSession;
import com.careerpick.service.CommunityMessageService;
import com.careerpick.service.CommunityMessageService.StoredMessage;
import com.careerpick.websocket.CommunityWebSocketConnections.Connection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.UUID;

/**
 * 프론트엔드가 보낸 WebSocket JSON 요청을 받고 인증·저장 흐름을 실행한다.
 * 백엔드 응답은 FrameFactory로 생성하고 Connections에 전달해 프론트엔드로 보낸다.
 */
@Component
public class CommunityWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(CommunityWebSocketHandler.class);
    private static final CloseStatus TRY_AGAIN_LATER = new CloseStatus(1013, "Try Again Later");
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final CommunityMessageService messageService;
    private final CommunityWebSocketConnections connections;
    private final CommunityWebSocketBroadcaster broadcaster;

    public CommunityWebSocketHandler(ObjectMapper objectMapper, AuthService authService,
                                     CommunityMessageService messageService,
                                     CommunityWebSocketConnections connections,
                                     CommunityWebSocketBroadcaster broadcaster) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.messageService = messageService;
        this.connections = connections;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connections.register(session, this::closeUnauthenticated);
    }

    /** 프론트엔드에서 백엔드로 들어오는 모든 WebSocket 텍스트 메시지의 진입점이다. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Connection connection = connections.find(session.getId());
        if (connection == null) return;

        JsonNode requestFrame = parseFrame(message);
        if (requestFrame == null) {
            rejectMalformedFrame(connection);
            return;
        }

        // 인증 전에는 AUTH만, 인증 후에는 실제 기능 프레임만 허용한다.
        if (!connection.isAuthenticated()) {
            handleAuthenticationFrame(connection, requestFrame);
            return;
        }
        handleAuthenticatedFrame(connection, requestFrame);
    }

    //인증 전
    private void handleAuthenticationFrame(Connection connection, JsonNode requestFrame) {
        if (!"AUTH".equals(text(requestFrame, "type"))) {
            reject(connection, "AUTH_REQUIRED", "먼저 인증해야 합니다.", null,
                CloseStatus.POLICY_VIOLATION);
            return;
        }
        authenticate(connection, text(requestFrame, "token"));
    }
    
    //인증 후 
    private void handleAuthenticatedFrame(Connection connection, JsonNode requestFrame) {
        if ("SEND_MESSAGE".equals(text(requestFrame, "type"))) {
            sendMessage(connection, requestFrame);
            return;
        }
        reject(connection, "UNSUPPORTED_TYPE", "지원하지 않는 메시지 타입입니다.", null, null);
    }

    private JsonNode parseFrame(TextMessage message) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception exception) {
            return null;
        }
    }

    private void rejectMalformedFrame(Connection connection) {
        boolean authenticated = connection.isAuthenticated();
        reject(connection,
            authenticated ? "VALIDATION_FAILED" : "AUTH_REQUIRED",
            authenticated ? "올바른 JSON 메시지가 아닙니다." : "먼저 인증해야 합니다.",
            null,
            authenticated ? null : CloseStatus.POLICY_VIOLATION);
    }

    private void authenticate(Connection connection, String token) {
        if (token == null || token.isBlank()) {
            reject(connection, "AUTH_REQUIRED", "로그인이 필요합니다.", null,
                CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            AuthenticatedSession auth = authService.requireWebSocketSession(token);
            connections.authenticate(connection, auth, this::expire);
            connections.send(connection, CommunityWebSocketFrameFactory.authenticated(auth));
        } catch (ResponseStatusException exception) {
            reject(connection, "AUTH_REQUIRED", "로그인이 만료되었거나 올바르지 않습니다.", null,
                CloseStatus.POLICY_VIOLATION);
        } catch (DataAccessException exception) {
            reject(connection, "SERVICE_UNAVAILABLE", "인증 서비스를 사용할 수 없습니다.", null,
                TRY_AGAIN_LATER);
        }
    }

    private void sendMessage(Connection connection, JsonNode requestFrame) {
        String clientMessageId = text(requestFrame, "clientMessageId");
        String canonicalId = canonicalUuid(clientMessageId);
        if (canonicalId == null) {
            reject(connection, "VALIDATION_FAILED", "clientMessageId가 올바르지 않습니다.",
                clientMessageId, null);
            return;
        }

        Long roomId = positiveLong(requestFrame, "roomId");
        String messageText = text(requestFrame, "text");
        if (roomId == null || messageText == null || messageText.trim().isEmpty() || messageText.trim().length() > 120) {
            reject(connection, "VALIDATION_FAILED", "방 ID와 1~120자의 메시지를 확인해 주세요.",
                canonicalId, null);
            return;
        }

        try {
            AuthenticatedSession current = authService.requireWebSocketSession(connection.auth().token());
            StoredMessage stored = messageService.store(
                current.userId(), roomId, canonicalId, messageText.trim());
            // 별도 @Transactional 서비스가 반환된 시점에 저장이 커밋되어 이후에만 전달한다.
            boolean published = broadcaster.broadcast(stored);
            connections.send(connection, CommunityWebSocketFrameFactory.sendResult(stored, published));
        } catch (ResponseStatusException exception) {
            handleRequestFailure(connection, canonicalId, exception);
        } catch (DataAccessException exception) {
            log.error("Failed to store or deliver a community message", exception);
            reject(connection, "MESSAGE_STORE_FAILED", "메시지를 저장하지 못했습니다.", canonicalId, null);
        } catch (RuntimeException exception) {
            log.error("Unexpected community message failure", exception);
            reject(connection, "MESSAGE_STORE_FAILED", "메시지를 저장하지 못했습니다.", canonicalId, null);
        }
    }

    private void handleRequestFailure(Connection connection, String clientMessageId,
                                      ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        boolean forbidden = status == 403;
        reject(connection,
            forbidden ? "NOT_A_MEMBER" : "AUTH_REQUIRED",
            forbidden ? "참여 중인 채팅방이 아닙니다." : "로그인이 만료되었습니다.",
            clientMessageId,
            status == 401 ? CloseStatus.POLICY_VIOLATION : null);
    }

    private String canonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString();
        } catch (Exception exception) {
            return null;
        }
    }

    private void closeUnauthenticated(Connection connection) {
        if (!connection.isAuthenticated()) {
            reject(connection, "AUTH_REQUIRED", "인증 시간이 초과되었습니다.", null,
                CloseStatus.POLICY_VIOLATION);
        }
    }

    private void expire(Connection connection) {
        reject(connection, "SESSION_EXPIRED", "로그인이 만료되었습니다.", null,
            CloseStatus.POLICY_VIOLATION);
    }

    private void reject(Connection connection, String code, String message, String clientMessageId,
                        CloseStatus closeStatus) {
        connections.send(connection, CommunityWebSocketFrameFactory.error(code, message, clientMessageId));
        if (closeStatus != null) connections.close(connection, closeStatus);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connections.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        connections.close(session, CloseStatus.SERVER_ERROR);
    }

    private String text(JsonNode requestFrame, String field) {
        JsonNode value = requestFrame.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private Long positiveLong(JsonNode requestFrame, String field) {
        JsonNode value = requestFrame.get(field);
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0
            ? value.longValue() : null;
    }

}
