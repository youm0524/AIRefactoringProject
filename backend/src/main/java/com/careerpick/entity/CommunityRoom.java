package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_rooms", uniqueConstraints =
    @UniqueConstraint(name = "uk_community_rooms_company_name", columnNames = "company_name"))
public class CommunityRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_name", nullable = false, length = 100) private String companyName;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected CommunityRoom() {}

    public CommunityRoom(String companyName) {
        this.companyName = companyName;
        this.createdAt = Instant.now();
    }

    public long id() { return id; }
    public String companyName() { return companyName; }
    public Instant createdAt() { return createdAt; }
}
