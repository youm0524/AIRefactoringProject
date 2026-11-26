package com.careerpick.websocket;

import com.careerpick.service.AuthService.AuthenticatedSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunityWebSocketConnectionsTest {
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final ScheduledFuture<?> authTimeout = mock(ScheduledFuture.class);
    private final ScheduledFuture<?> expiry = mock(ScheduledFuture.class);
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final CommunityWebSocketConnections connections = new CommunityWebSocketConnections(
        new ObjectMapper(), scheduler);

    @Test void cancelsConnectionTimersWhenRemoved() {
        when(session.getId()).thenReturn("socket-1");
        doReturn(authTimeout, expiry).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        CommunityWebSocketConnections.Connection connection = connections.register(session, ignored -> {});
        connections.authenticate(connection,
            new AuthenticatedSession(1, "김커리", "token", Instant.now().plusSeconds(60)), ignored -> {});

        connections.remove("socket-1");

        assertThat(connections.find("socket-1")).isNull();
        verify(authTimeout).cancel(false);
        verify(expiry).cancel(false);
    }
}
