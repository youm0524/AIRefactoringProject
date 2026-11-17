package com.careerpick.websocket;

import com.careerpick.repository.CommunityRoomMemberRepository;
import com.careerpick.service.AuthService;
import com.careerpick.service.CommunityMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunityWebSocketHandlerTest {
    private final AuthService auth = mock(AuthService.class);
    private final CommunityMessageService messages = mock(CommunityMessageService.class);
    private final CommunityRoomMemberRepository members = mock(CommunityRoomMemberRepository.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final ScheduledFuture<?> future = mock(ScheduledFuture.class);
    private final CommunityWebSocketHandler handler = new CommunityWebSocketHandler(
        new ObjectMapper(), auth, messages, members, scheduler);

    @BeforeEach void setUp() {
        when(session.getId()).thenReturn("socket-1");
        when(session.isOpen()).thenReturn(true);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        handler.afterConnectionEstablished(session);
    }

    @Test void requiresAuthenticationBeforeSending() throws Exception {
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"SEND_MESSAGE\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("AUTH_REQUIRED"));
        verifyNoInteractions(messages);
    }

    @Test void closesWhenTheFirstFrameIsNotJsonAuthentication() throws Exception {
        handler.handleTextMessage(session, new TextMessage("not-json"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("AUTH_REQUIRED"));
    }

    @Test void closesWhenAuthenticationExpiresBeforeTheFirstFrame() throws Exception {
        ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(timeout.capture(), any(Instant.class));

        timeout.getValue().run();

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("AUTH_REQUIRED"));
    }

    @Test void rejectsAnExpiredToken() throws Exception {
        when(auth.requireWebSocketSession("expired"))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\",\"token\":\"expired\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("AUTH_REQUIRED"));
    }

    @Test void storesThenPublishesMessageToCurrentMembers() throws Exception {
        Instant expiry = Instant.now().plusSeconds(60);
        when(auth.requireWebSocketSession("token"))
            .thenReturn(new AuthService.AuthenticatedSession(1, "김커리", "token", expiry));
        when(messages.store(1, 20, "8fb79fd8-c912-4aa2-a994-f10c13664570", "안녕하세요"))
            .thenReturn(new CommunityMessageService.StoredMessage(
                30, 20, 1, "김커리", "안녕하세요", Instant.parse("2026-11-17T00:00:00Z"),
                "8fb79fd8-c912-4aa2-a994-f10c13664570"));
        when(members.findUserIdsByRoomIdAndUserIdIn(20, Set.of(1L))).thenReturn(Set.of(1L));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\",\"token\":\"token\"}"));
        handler.handleTextMessage(session, new TextMessage("""
            {"type":"SEND_MESSAGE","roomId":20,
             "clientMessageId":"8fb79fd8-c912-4aa2-a994-f10c13664570","text":" 안녕하세요 "}
            """));

        assertThat(sentFrames()).anyMatch(frame -> frame.contains("AUTHENTICATED"));
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("MESSAGE_CREATED"));
        assertThat(sentFrames()).anyMatch(frame -> frame.contains("SEND_RESULT") && frame.contains("PUBLISHED"));
        verify(messages).store(1, 20, "8fb79fd8-c912-4aa2-a994-f10c13664570", "안녕하세요");
    }

    @Test void doesNotPublishWhenMessageStorageFails() throws Exception {
        when(auth.requireWebSocketSession("token"))
            .thenReturn(new AuthService.AuthenticatedSession(1, "김커리", "token", Instant.now().plusSeconds(60)));
        when(messages.store(anyLong(), anyLong(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\",\"token\":\"token\"}"));

        handler.handleTextMessage(session, new TextMessage("""
            {"type":"SEND_MESSAGE","roomId":20,
             "clientMessageId":"8fb79fd8-c912-4aa2-a994-f10c13664570","text":"안녕하세요"}
            """));

        assertThat(sentFrames()).anyMatch(frame -> frame.contains("MESSAGE_STORE_FAILED"));
        verifyNoInteractions(members);
    }

    @Test void rejectsInvalidMessageFieldsBeforeStorage() throws Exception {
        when(auth.requireWebSocketSession("token"))
            .thenReturn(new AuthService.AuthenticatedSession(1, "김커리", "token", Instant.now().plusSeconds(60)));
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\",\"token\":\"token\"}"));

        handler.handleTextMessage(session, new TextMessage("""
            {"type":"SEND_MESSAGE","roomId":20,"clientMessageId":"not-uuid","text":"메시지"}
            """));
        handler.handleTextMessage(session, new TextMessage("""
            {"type":"SEND_MESSAGE","roomId":20.5,
             "clientMessageId":"8fb79fd8-c912-4aa2-a994-f10c13664570","text":"메시지"}
            """));
        handler.handleTextMessage(session, new TextMessage("""
            {"type":"SEND_MESSAGE","roomId":20,
             "clientMessageId":"8fb79fd8-c912-4aa2-a994-f10c13664570","text":""}
            """));

        assertThat(sentFrames().stream().filter(frame -> frame.contains("VALIDATION_FAILED"))).hasSize(3);
        verifyNoInteractions(messages);
    }

    private java.util.List<String> sentFrames() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues().stream().map(TextMessage::getPayload).toList();
    }
}
