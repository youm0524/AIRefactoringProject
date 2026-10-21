package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_messages_job_id", columnList = "job_id,id"))
public class ChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 50) private String author;
    @Column(nullable = false, length = 120) private String text;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ChatMessageEntity() {}

    public ChatMessageEntity(Job job, User user, String author, String text) {
        this.job = job;
        this.user = user;
        this.author = author;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public long id() { return id; }
    public String author() { return author; }
    public String text() { return text; }
}
