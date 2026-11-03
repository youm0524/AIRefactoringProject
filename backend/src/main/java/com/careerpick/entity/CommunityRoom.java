package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_rooms", uniqueConstraints =
    @UniqueConstraint(name = "uk_community_rooms_company_id", columnNames = "company_id"))
public class CommunityRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected CommunityRoom() {}

    public CommunityRoom(Company company) {
        this.company = company;
        this.createdAt = Instant.now();
    }

    public long id() { return id; }
    public Company company() { return company; }
    public Instant createdAt() { return createdAt; }
}
