package com.careerpick.websocket;

import com.careerpick.service.AuthService.AuthenticatedSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

/** 현재 서버 인스턴스의 WebSocket 연결 상태와 수명만 관리한다. */
@Component
public class CommunityWebSocketConnections {
    private final ObjectMapper objectMapper;
    private final TaskScheduler scheduler;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public CommunityWebSocketConnections(ObjectMapper objectMapper, TaskScheduler scheduler) {
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    Connection register(WebSocketSession session, Consumer<Connection> onAuthTimeout) {
        session.setTextMessageSizeLimit(2048);
        Connection connection = new Connection(session);
        connections.put(session.getId(), connection);
        // 인증되지 않은 연결이 서버 자원을 계속 점유하지 않도록 최초 AUTH 시간을 제한한다.
        connection.authTimeout = scheduler.schedule(() -> {
            if (!connection.isAuthenticated()) onAuthTimeout.accept(connection);
        }, Instant.now().plusSeconds(5));
        return connection;
    }

    Connection find(String sessionId) {
        return connections.get(sessionId);
    }

    void authenticate(Connection connection, AuthenticatedSession auth, Consumer<Connection> onExpiry) {
        connection.authenticate(auth);
        cancel(connection.authTimeout);
        connection.authTimeout = null;
        // 열린 소켓이 Redis 로그인 세션의 TTL을 넘어 유지되지 않도록 만료 종료를 예약한다.
        connection.expiry = scheduler.schedule(() -> onExpiry.accept(connection), auth.expiresAt());
    }

    List<Connection> authenticated() {
        return connections.values().stream()
            .filter(connection -> connection.isAuthenticated() && connection.session.isOpen())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    boolean send(Connection connection, Object responseFrame) {
        WebSocketSession session = connection.session;
        if (!session.isOpen()) return false;
        try {
            // Spring WebSocketSession은 동시 전송을 보장하지 않으므로 연결별 쓰기를 직렬화한다.
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseFrame)));
            }
            return true;
        } catch (IOException exception) {
            close(connection, CloseStatus.SERVER_ERROR);
            return false;
        }
    }

    void close(WebSocketSession session, CloseStatus status) {
        Connection connection = find(session.getId());
        if (connection != null) close(connection, status);
    }

    void close(Connection connection, CloseStatus status) {
        try {
            if (connection.session.isOpen()) connection.session.close(status);
        } catch (IOException ignored) {
            // The connection is already unusable.
        } finally {
            remove(connection.session.getId());
        }
    }

    void remove(String sessionId) {
        Connection removed = connections.remove(sessionId);
        if (removed == null) return;
        // 종료된 연결이 예약 작업에 계속 참조되지 않도록 두 타이머를 함께 해제한다.
        cancel(removed.authTimeout);
        cancel(removed.expiry);
    }

    private void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    static final class Connection {
        private final WebSocketSession session;
        private volatile AuthenticatedSession auth;
        private volatile ScheduledFuture<?> authTimeout;
        private volatile ScheduledFuture<?> expiry;

        Connection(WebSocketSession session) {
            this.session = session;
        }

        WebSocketSession session() { return session; }
        AuthenticatedSession auth() { return auth; }
        boolean isAuthenticated() { return auth != null; }
        void authenticate(AuthenticatedSession value) { auth = value; }
    }
}
