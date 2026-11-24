package com.careerpick.service;

import com.careerpick.dto.JoinCommunityResponse;
import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.Company;
import com.careerpick.entity.CommunityRoom;
import com.careerpick.entity.CommunityRoomMember;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import com.careerpick.repository.CommunityRoomMemberRepository;
import com.careerpick.repository.CommunityRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityServiceTest {
    private final AuthService authService = mock(AuthService.class);
    private final JobService jobService = mock(JobService.class);
    private final CommunityRoomMemberRepository members = mock(CommunityRoomMemberRepository.class);
    private final CommunityRoomRepository rooms = mock(CommunityRoomRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final CommunityService service = new CommunityService(authService, jobService, members, rooms, messages);
    private final User user = user();

    @Test void joinsTheRoomConnectedToTheJob() {
        CommunityRoom room = room();
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(jobService.findById(10)).thenReturn(job(room.company()));
        when(rooms.findByCompanyId(30)).thenReturn(Optional.of(room));

        JoinCommunityResponse response = service.join(10, "Bearer token");

        assertThat(response.room().id()).isEqualTo(20);
        assertThat(response.room().companyId()).isEqualTo(30);
        assertThat(response.room().companyName()).isEqualTo("네오테크");
        verify(members).join(1, 20);
    }

    @Test void rejectsMissingJobWithoutCreatingMembership() {
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);

        assertThatThrownBy(() -> service.join(999, "Bearer token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(members);
    }

    @Test void leavesAnExistingRoom() {
        CommunityRoomMember membership = member(room(), "2026-10-01T00:00:00Z");
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(members.findForUpdate(1, 20)).thenReturn(Optional.of(membership));

        service.leave(20, "Bearer token");

        verify(members).delete(membership);
    }

    @Test void leavingAnUnjoinedRoomIsIdempotent() {
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(members.findForUpdate(1, 999)).thenReturn(Optional.empty());

        service.leave(999, "Bearer token");

        verify(members, never()).delete(any());
    }

    @Test void returnsLatestMessagesInOldestFirstOrder() {
        CommunityRoom room = room(20, "네오테크");
        ChatMessageEntity older = message(1, room, user, "이전", "2026-10-01T00:00:00Z");
        ChatMessageEntity newer = message(2, room, null, "최신", "2026-10-02T00:00:00Z");
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(rooms.existsById(20L)).thenReturn(true);
        when(members.existsByUserIdAndRoomId(1, 20)).thenReturn(true);
        when(messages.findTop50ByRoomIdOrderByCreatedAtDescIdDesc(20)).thenReturn(List.of(newer, older));

        var result = service.messages(20, "Bearer token");

        assertThat(result).extracting(message -> message.id()).containsExactly(1L, 2L);
        assertThat(result).extracting(message -> message.mine()).containsExactly(true, false);
    }

    @Test void rejectsMessagesForNonMember() {
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(rooms.existsById(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.messages(20, "Bearer token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
        verifyNoInteractions(messages);
    }

    @Test void sortsActiveRoomsBeforeEmptyRooms() {
        CommunityRoom activeLatest = room(20, "네오테크");
        CommunityRoom activeOlder = room(21, "오늘마켓");
        CommunityRoom emptyRecent = room(22, "빈방A");
        CommunityRoom emptyOlder = room(23, "빈방B");
        CommunityRoomMember emptyOlderMember = member(emptyOlder, "2026-10-01T00:00:00Z");
        CommunityRoomMember activeOlderMember = member(activeOlder, "2026-10-02T00:00:00Z");
        CommunityRoomMember emptyRecentMember = member(emptyRecent, "2026-10-04T00:00:00Z");
        CommunityRoomMember activeLatestMember = member(activeLatest, "2026-10-03T00:00:00Z");
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(members.findByUserId(1)).thenReturn(List.of(
            emptyOlderMember, activeOlderMember, emptyRecentMember, activeLatestMember));
        when(messages.findTop1ByRoomIdOrderByCreatedAtDescIdDesc(20))
            .thenReturn(List.of(message(2, activeLatest, null, "최신", "2026-10-06T00:00:00Z")));
        when(messages.findTop1ByRoomIdOrderByCreatedAtDescIdDesc(21))
            .thenReturn(List.of(message(1, activeOlder, null, "이전", "2026-10-05T00:00:00Z")));
        when(messages.findTop1ByRoomIdOrderByCreatedAtDescIdDesc(22)).thenReturn(List.of());
        when(messages.findTop1ByRoomIdOrderByCreatedAtDescIdDesc(23)).thenReturn(List.of());

        var result = service.rooms("Bearer token");

        assertThat(result).extracting(roomSummary -> roomSummary.id()).containsExactly(20L, 21L, 22L, 23L);
        assertThat(result.get(0).companyId()).isEqualTo(30);
    }

    private User user() {
        User user = new User("김커리", "demo@careerpick.kr", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private CommunityRoom room() {
        return room(20, "네오테크");
    }

    private CommunityRoom room(long id, String companyName) {
        Company company = new Company(companyName, companyName.substring(0, 1), "#000000");
        ReflectionTestUtils.setField(company, "id", id + 10);
        CommunityRoom room = new CommunityRoom(company);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private CommunityRoomMember member(CommunityRoom room, String joinedAt) {
        CommunityRoomMember member = mock(CommunityRoomMember.class);
        when(member.room()).thenReturn(room);
        when(member.joinedAt()).thenReturn(Instant.parse(joinedAt));
        return member;
    }

    private ChatMessageEntity message(long id, CommunityRoom room, User author, String text, String createdAt) {
        ChatMessageEntity message = new ChatMessageEntity(room, null, author, author == null ? "익명" : author.name(), text);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse(createdAt));
        return message;
    }

    private Job job(Company company) {
        return new Job(company, "개발자 채용", "개발", "신입", "서울", "D-3", List.of("백엔드"));
    }
}
