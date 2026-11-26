package com.careerpick.websocket;

import com.careerpick.service.AuthService.AuthenticatedSession;
import com.careerpick.service.CommunityMessageService;
import com.careerpick.service.CommunityMessageService.StoredMessage;
import com.careerpick.websocket.CommunityWebSocketConnections.Connection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunityWebSocketBroadcasterTest {
    private final CommunityMessageService messages = mock(CommunityMessageService.class);
    private final CommunityWebSocketConnections connections = mock(CommunityWebSocketConnections.class);
    private final CommunityWebSocketBroadcaster broadcaster = new CommunityWebSocketBroadcaster(messages, connections);

    @Test void sendsOnlyToCurrentMembersAndCalculatesMinePerConnection() {
        Connection sender = connection("sender", 1);
        Connection recipient = connection("recipient", 2);
        Connection nonMember = connection("non-member", 3);
        StoredMessage message = new StoredMessage(
            30, 20, 1, "김커리", "안녕하세요", Instant.parse("2026-11-17T00:00:00Z"), "client-id");
        when(connections.authenticated()).thenReturn(List.of(sender, recipient, nonMember));
        when(messages.findRecipientUserIds(20, Set.of(1L, 2L, 3L))).thenReturn(Set.of(1L, 2L));
        when(connections.send(any(), any())).thenReturn(true);

        assertThat(broadcaster.broadcast(message)).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> frames = ArgumentCaptor.forClass(Map.class);
        verify(connections).send(eq(sender), frames.capture());
        verify(connections).send(eq(recipient), frames.capture());
        verify(connections, never()).send(eq(nonMember), any());
        assertThat(messageView(frames.getAllValues().get(0))).containsEntry("mine", true);
        assertThat(messageView(frames.getAllValues().get(1))).containsEntry("mine", false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> messageView(Map<String, Object> frame) {
        return (Map<String, Object>) frame.get("message");
    }

    private Connection connection(String id, long userId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        Connection connection = new Connection(session);
        connection.authenticate(new AuthenticatedSession(userId, "사용자" + userId, "token" + userId,
            Instant.now().plusSeconds(60)));
        return connection;
    }
}
