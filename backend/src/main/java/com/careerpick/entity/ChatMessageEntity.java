package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes =
    @Index(name = "idx_chat_messages_room_created", columnList = "room_id,created_at,id"),
    uniqueConstraints = @UniqueConstraint(name = "uk_chat_messages_user_client", columnNames = {"user_id", "client_message_id"}))
public class ChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_job_id")
    private Job sourceJob;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private CommunityRoom room;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "client_message_id", length = 36) private String clientMessageId;
    @Column(nullable = false, length = 50) private String author;
    @Column(nullable = false, length = 120) private String text;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ChatMessageEntity() {}

    public ChatMessageEntity(CommunityRoom room, Job sourceJob, User user, String author, String text) {
        this.room = room;
        this.sourceJob = sourceJob;
        this.user = user;
        this.author = author;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public long id() { return id; }
    public CommunityRoom room() { return room; }
    public User user() { return user; }
    public String clientMessageId() { return clientMessageId; }
    public String author() { return author; }
    public String text() { return text; }
    public Instant createdAt() { return createdAt; }
}
