package com.careerpick.service;

import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.Company;
import com.careerpick.entity.CommunityRoom;
import com.careerpick.entity.CommunityRoomMember;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import com.careerpick.repository.CommunityRoomMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityMessageServiceTest {
    private final CommunityRoomMemberRepository members = mock(CommunityRoomMemberRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final CommunityMessageService service = new CommunityMessageService(members, messages);
    private final User user = user();
    private final CommunityRoom room = room();
    private final CommunityRoomMember member = member();

    @Test void storesMessageForLockedMember() {
        when(members.findForUpdate(1, 20)).thenReturn(Optional.of(member));
        when(messages.findByUserIdAndClientMessageId(1, "client-id")).thenReturn(Optional.empty());
        when(messages.save(any())).thenAnswer(invocation -> {
            ChatMessageEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        var result = service.store(1, 20, "client-id", "안녕하세요");

        assertThat(result.id()).isEqualTo(30);
        assertThat(result.roomId()).isEqualTo(20);
        assertThat(result.author()).isEqualTo("김커리");
        assertThat(result.text()).isEqualTo("안녕하세요");
    }

    @Test void returnsExistingMessageForDuplicateClientId() {
        ChatMessageEntity existing = new ChatMessageEntity(room, user, "client-id", user.name(), "기존 메시지");
        ReflectionTestUtils.setField(existing, "id", 30L);
        when(members.findForUpdate(1, 20)).thenReturn(Optional.of(member));
        when(messages.findByUserIdAndClientMessageId(1, "client-id")).thenReturn(Optional.of(existing));

        assertThat(service.store(1, 20, "client-id", "재전송").text()).isEqualTo("기존 메시지");
        verify(messages, never()).save(any());
    }

    @Test void rejectsNonMemberBeforeStoring() {
        when(members.findForUpdate(1, 20)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.store(1, 20, "client-id", "메시지"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
        verifyNoInteractions(messages);
    }

    private User user() {
        User value = new User("김커리", "demo@careerpick.kr", "hash");
        ReflectionTestUtils.setField(value, "id", 1L);
        return value;
    }

    private CommunityRoom room() {
        Company company = new Company("네오테크", "N", "#000000");
        ReflectionTestUtils.setField(company, "id", 10L);
        CommunityRoom value = new CommunityRoom(company);
        ReflectionTestUtils.setField(value, "id", 20L);
        return value;
    }

    private CommunityRoomMember member() {
        CommunityRoomMember value = mock(CommunityRoomMember.class);
        when(value.user()).thenReturn(user);
        when(value.room()).thenReturn(room);
        return value;
    }
}
