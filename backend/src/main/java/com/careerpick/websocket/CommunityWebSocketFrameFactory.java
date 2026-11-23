package com.careerpick.websocket;

import com.careerpick.service.AuthService.AuthenticatedSession;
import com.careerpick.service.CommunityMessageService.StoredMessage;
import java.util.LinkedHashMap;
import java.util.Map;

/** WebSocket 전송과 분리해 클라이언트에 공개되는 JSON 프레임 계약만 생성한다. */
final class CommunityWebSocketFrameFactory {
    private CommunityWebSocketFrameFactory() {}

    static Map<String, Object> authenticated(AuthenticatedSession auth) {
        return Map.of("type", "AUTHENTICATED", "expiresAt", auth.expiresAt().toString());
    }

    static Map<String, Object> messageCreated(StoredMessage message, long viewerId) {
        return Map.of("type", "MESSAGE_CREATED", "message", messageView(message, viewerId));
    }

    static Map<String, Object> sendResult(StoredMessage message, boolean published) {
        return Map.of(
            "type", "SEND_RESULT",
            "clientMessageId", message.clientMessageId(),
            "status", published ? "PUBLISHED" : "STORED_BUT_NOT_BROADCAST",
            "message", messageView(message, message.userId()));
    }

    static Map<String, Object> error(String code, String message, String clientMessageId) {
        Map<String, Object> responseFrame = new LinkedHashMap<>();
        responseFrame.put("type", "ERROR");
        responseFrame.put("code", code);
        responseFrame.put("message", message);
        if (clientMessageId != null) responseFrame.put("clientMessageId", clientMessageId);
        return responseFrame;
    }

    private static Map<String, Object> messageView(StoredMessage message, long viewerId) {
        return Map.of(
            "id", message.id(),
            "roomId", message.roomId(),
            "author", message.author(),
            "text", message.text(),
            "createdAt", message.createdAt().toString(),
            "mine", message.userId() == viewerId,
            "clientMessageId", message.clientMessageId());
    }
}
