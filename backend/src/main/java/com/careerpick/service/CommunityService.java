package com.careerpick.service;

import com.careerpick.dto.*;
import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.CommunityRoom;
import com.careerpick.entity.CommunityRoomMember;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import com.careerpick.repository.CommunityRoomMemberRepository;
import com.careerpick.repository.CommunityRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CommunityService {
    private final AuthService authService;
    private final JobService jobService;
    private final CommunityRoomMemberRepository members;
    private final CommunityRoomRepository rooms;
    private final ChatMessageRepository messages;

    public CommunityService(AuthService authService, JobService jobService, CommunityRoomMemberRepository members,
                            CommunityRoomRepository rooms, ChatMessageRepository messages) {
        this.authService = authService;
        this.jobService = jobService;
        this.members = members;
        this.rooms = rooms;
        this.messages = messages;
    }

    @Transactional
    public JoinCommunityResponse join(long jobId, String authorization) {
        User user = authService.requireUserEntity(authorization);
        Job job = jobService.findById(jobId);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");

        CommunityRoom room = rooms.findByCompanyId(job.company().id())
            .orElseThrow(() -> new IllegalStateException("기업 채팅방 연결이 없습니다."));
        members.join(user.id(), room.id());
        return new JoinCommunityResponse(new CommunityRoomView(
            room.id(), room.company().id(), room.company().name()));
    }

    @Transactional
    public void leave(long roomId, String authorization) {
        User user = authService.requireUserEntity(authorization);
        members.findForUpdate(user.id(), roomId).ifPresent(members::delete);
    }

    @Transactional(readOnly = true)
    public List<CommunityRoomSummary> rooms(String authorization) {
        User user = authService.requireUserEntity(authorization);
        // shortcut: 참여 방이 적은 MVP에서는 방별 최신 메시지를 조회하고, 방 수가 늘면 단일 projection 쿼리로 교체한다.
        return members.findByUserId(user.id()).stream().map(this::toSummary).sorted(this::compareRooms).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> messages(long roomId, String authorization) {
        User user = authService.requireUserEntity(authorization);
        if (!rooms.existsById(roomId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.");
        if (!members.existsByUserIdAndRoomId(user.id(), roomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 채팅방이 아닙니다.");
        }

        List<ChatMessageEntity> latest = new ArrayList<>(messages.findTop50ByRoomIdOrderByCreatedAtDescIdDesc(roomId));
        latest.sort(Comparator.comparing(ChatMessageEntity::createdAt).thenComparingLong(ChatMessageEntity::id));
        return latest.stream().map(message -> toMessage(message, user)).toList();
    }

    private CommunityRoomSummary toSummary(CommunityRoomMember member) {
        CommunityRoom room = member.room();
        LastMessageView lastMessage = messages.findTop1ByRoomIdOrderByCreatedAtDescIdDesc(room.id()).stream()
            .findFirst().map(this::toLastMessage).orElse(null);
        return new CommunityRoomSummary(
            room.id(), room.company().id(), room.company().name(), member.joinedAt(), lastMessage);
    }

    private LastMessageView toLastMessage(ChatMessageEntity message) {
        return new LastMessageView(message.id(), message.author(), message.text(), message.createdAt());
    }

    private ChatMessage toMessage(ChatMessageEntity message, User user) {
        boolean isMine = message.user() != null && message.user().id().equals(user.id());
        return new ChatMessage(message.id(), message.room().id(), message.author(), message.text(),
            message.createdAt(), isMine, message.clientMessageId());
    }

    private int compareRooms(CommunityRoomSummary left, CommunityRoomSummary right) {
        if (left.lastMessage() == null && right.lastMessage() != null) return 1;
        if (left.lastMessage() != null && right.lastMessage() == null) return -1;
        if (left.lastMessage() != null) {
            int time = right.lastMessage().createdAt().compareTo(left.lastMessage().createdAt());
            if (time != 0) return time;
            int message = Long.compare(right.lastMessage().id(), left.lastMessage().id());
            if (message != 0) return message;
        } else {
            int joined = right.joinedAt().compareTo(left.joinedAt());
            if (joined != 0) return joined;
        }
        return Long.compare(right.id(), left.id());
    }
}
