package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_room_members", uniqueConstraints =
    @UniqueConstraint(name = "uk_community_room_members_user_room", columnNames = {"user_id", "room_id"}),
    indexes = @Index(name = "idx_community_room_members_room_id", columnList = "room_id"))
public class CommunityRoomMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private CommunityRoom room;
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected CommunityRoomMember() {}

    public long id() { return id; }
    public User user() { return user; }
    public CommunityRoom room() { return room; }
    public Instant joinedAt() { return joinedAt; }
}
