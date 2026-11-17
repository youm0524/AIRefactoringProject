package com.careerpick.service;

import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.CommunityRoomMember;
import com.careerpick.repository.ChatMessageRepository;
import com.careerpick.repository.CommunityRoomMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class CommunityMessageService {
    private final CommunityRoomMemberRepository members;
    private final ChatMessageRepository messages;

    public CommunityMessageService(CommunityRoomMemberRepository members, ChatMessageRepository messages) {
        this.members = members;
        this.messages = messages;
    }

    @Transactional
    public StoredMessage store(long userId, long roomId, String clientMessageId, String text) {
        CommunityRoomMember member = members.findForUpdate(userId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 채팅방이 아닙니다."));
        ChatMessageEntity message = messages.findByUserIdAndClientMessageId(userId, clientMessageId)
            .orElseGet(() -> messages.save(new ChatMessageEntity(
                member.room(), member.user(), clientMessageId, member.user().name(), text)));
        return StoredMessage.from(message);
    }

    public record StoredMessage(long id, long roomId, long userId, String author, String text,
                                Instant createdAt, String clientMessageId) {
        private static StoredMessage from(ChatMessageEntity message) {
            return new StoredMessage(message.id(), message.room().id(), message.user().id(), message.author(),
                message.text(), message.createdAt(), message.clientMessageId());
        }
    }
}
