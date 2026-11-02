package com.careerpick.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "cover_letters",
    uniqueConstraints = @UniqueConstraint(name = "uk_cover_letters_user_job", columnNames = {"user_id", "job_id"}),
    indexes = @Index(name = "idx_cover_letters_user_updated", columnList = "user_id,updated_at,id"))
public class CoverLetter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "coverLetter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<CoverLetterItem> items = new ArrayList<>();

    protected CoverLetter() {}

    public CoverLetter(User user, Job job) {
        this.user = Objects.requireNonNull(user);
        this.job = Objects.requireNonNull(job);
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.updatedAt = createdAt;
        items.add(new CoverLetterItem(this, "motivation", "지원 동기와 입사 후 이루고 싶은 목표를 작성해 주세요.", 1, 700));
        items.add(new CoverLetterItem(this, "experience", "직무와 관련해 가장 몰입했던 경험을 작성해 주세요.", 2, 700));
    }

    public void updateAnswers(Map<Long, String> answers) {
        Set<Long> itemIds = items.stream().map(CoverLetterItem::id).collect(Collectors.toSet());
        if (answers == null || itemIds.contains(null) || !itemIds.equals(answers.keySet())) {
            throw new IllegalArgumentException("모든 문항의 답변이 필요합니다.");
        }
        // 모든 답변을 먼저 검증해 일부 문항만 변경되는 것을 방지한다.
        items.forEach(item -> item.validateAnswer(answers.get(item.id())));
        items.forEach(item -> item.updateAnswer(answers.get(item.id())));
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plus(1, ChronoUnit.MICROS);
    }

    public Long id() { return id; }
    public User user() { return user; }
    public Job job() { return job; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public List<CoverLetterItem> items() { return List.copyOf(items); }
}
