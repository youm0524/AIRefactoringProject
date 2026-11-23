package com.careerpick.websocket;

import com.careerpick.service.CommunityMessageService;
import com.careerpick.service.CommunityMessageService.StoredMessage;
import com.careerpick.websocket.CommunityWebSocketConnections.Connection;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 저장된 메시지를 현재 서버에 연결된 방 참여자에게만 전달한다. */
@Component
public class CommunityWebSocketBroadcaster {
    private final CommunityMessageService messages;
    private final CommunityWebSocketConnections connections;

    public CommunityWebSocketBroadcaster(CommunityMessageService messages,
                                         CommunityWebSocketConnections connections) {
        this.messages = messages;
        this.connections = connections;
    }

    public boolean broadcast(StoredMessage message) {
        List<Connection> authenticated = connections.authenticated();
        if (authenticated.isEmpty()) return true;

        Set<Long> connectedUserIds = authenticated.stream()
            .map(connection -> connection.auth().userId())
            .collect(Collectors.toSet());
        // 방에서 나간 연결로 전달되지 않도록 송신 직전에 현재 참여 관계를 다시 확인한다.
        Set<Long> recipients = messages.findRecipientUserIds(message.roomId(), connectedUserIds);

        boolean delivered = true;
        for (Connection connection : authenticated) {
            if (!recipients.contains(connection.auth().userId())) continue;
            delivered &= connections.send(connection,
                CommunityWebSocketFrameFactory.messageCreated(message, connection.auth().userId()));
        }
        return delivered;
    }
}
